// =============================================================================
// Nekko — brainstorm Edge Function
// -----------------------------------------------------------------------------
// Generates personalized conversation suggestions for a contact from their real
// notes + relationship info, persists them in brainstorm_sessions /
// brainstorm_topics, and returns them to the client.
//
// Ownership is verified from the caller's JWT (service role bypasses RLS, so we
// enforce `contact.owner_id = auth.uid()` ourselves). A one-per-contact-per-day
// cooldown short-circuits before any LLM call.
//
// Deploy:
//   supabase secrets set GEMINI_API_KEY=<google-ai-studio-key>
//   supabase functions deploy brainstorm --no-verify-jwt
// =============================================================================

import { createClient } from "npm:@supabase/supabase-js@2";
import { complete } from "./llm.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const admin = createClient(supabaseUrl, serviceRoleKey, {
  auth: { persistSession: false },
});

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
};

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });

// Tone handling for sensitive content is a hard requirement, not polish.
const SYSTEM_PROMPT = `You are Brainstorm, a warm and thoughtful conversation assistant inside the Nekko relationship app.
You are given notes and relationship context about a specific contact. Generate 4 to 6 specific, personalized suggestions of things the user could actually say or ask this contact — grounded in the real notes, not generic topic categories.
Each suggestion must be a concrete, ready-to-use phrase or question (1–2 sentences), tailored to what is in the notes.
If the notes reference an upcoming date, milestone, event, or anything time-sensitive, fold that directly into at least one suggestion.

Tone rules — CRITICAL:
- If the notes mention loss, grief, conflict, breakups, health issues, or other emotionally heavy topics, respond with warmth and restraint. Suggest gentle, open-ended ways to check in. Never be glib, presumptuous, or joke about serious situations. When in doubt, suggest something simple and low-pressure rather than something specific that could land wrong.
- Keep the tone natural and human, the way a close friend would gently bring something up.
- Never invent facts about the contact that are not in the notes. If the notes are thin, lean toward simple, safe, open-ended suggestions.
- Do not reveal that your suggestions come from an AI or from reading private notes.

Return ONLY a single JSON array with 4 to 6 objects, each shaped:
{"title":"short 1-3 word label","description":"the exact thing to say or ask"}
Rules: no markdown code fence, no prose before or after, no trailing text.
Use valid JSON only — double quotes, escape any internal quotes, and do NOT put raw line breaks inside a string value.`;
// Safe, low-pressure fallback used when a contact has no notes/context yet
// (no LLM call — free and instant).
const FALLBACK_TOPICS = [
  { title: "Catch up", description: "Ask what's new with them and what they've been up to lately." },
  { title: "How are they doing", description: "Check in on how they're doing and really listen to the answer." },
  { title: "Reconnect", description: "Send a quick, warm message just to let them know you're thinking of them." },
  { title: "Make plans", description: "Suggest a low-pressure way to catch up, like a short call or grabbing coffee." },
];

function buildPrompt(
  contact: { name: string; check_in_frequency: string | null; last_check_in_date: string | null; next_check_in_date: string | null; streak_count: number | null },
  notes: { title: string; body: string | null; created_at: string }[],
  checkIns: { note: string | null }[],
  reminders: { title: string; description: string | null }[],
): string {
  const parts: string[] = [];
  parts.push(`Contact: ${contact.name}`);
  if (contact.check_in_frequency) parts.push(`Check-in cadence: ${contact.check_in_frequency}`);
  if (contact.streak_count) parts.push(`Current check-in streak: ${contact.streak_count}`);
  if (contact.last_check_in_date) parts.push(`Last check-in: ${contact.last_check_in_date}`);

  if (notes.length) {
    parts.push("\nNotes:");
    notes.forEach((n, i) => {
      parts.push(`${i + 1}. "${n.title}"${n.body ? " — " + n.body : ""}`);
    });
  }
  if (checkIns.length) {
    parts.push("\nRecent check-in notes:");
    checkIns.forEach((c, i) => {
      if (c.note) parts.push(`${i + 1}. ${c.note}`);
    });
  }
  if (reminders.length) {
    parts.push("\nReminders set for them:");
    reminders.forEach((r) => parts.push(`- ${r.title}${r.description ? ": " + r.description : ""}`));
  }

  return `${SYSTEM_PROMPT}\n\nContact context:\n${parts.join("\n")}`;
}

// Pull a usable array out of the model's text. Tolerates ``` fences, leading /
// trailing prose, a wrapping {"topics":[...]} object, brackets inside string
// values, and stray raw newlines inside strings. Returns null if nothing usable.
function parseTopics(raw: string): { title: string; description: string | null; icon: string | null }[] | null {
  const jsonText = extractJsonValue(raw);
  if (jsonText === null) {
    return null;
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(jsonText);
  } catch {
    try {
      parsed = JSON.parse(repairJson(jsonText));
    } catch {
      return null;
    }
  }
  const arr = Array.isArray(parsed)
    ? parsed
    : Array.isArray((parsed as any)?.topics) ? (parsed as any).topics
    : Array.isArray((parsed as any)?.suggestions) ? (parsed as any).suggestions
    : null;
  if (!Array.isArray(arr)) return null;
  const items = arr
    .filter((x: any) => x && typeof x.title === "string" && x.title.trim())
    .map((x: any) => ({
      title: String(x.title).trim(),
      description: typeof x.description === "string" ? x.description.trim() : null,
      icon: typeof x.icon === "string" ? x.icon.trim() : null,
    }));
  return items.length ? items : null;
}

// Returns the first balanced JSON value ('[...]' or '{...}') in raw, respecting
// JSON string literals so brackets inside quotes are ignored.
function extractJsonValue(raw: string): string | null {
  const text = raw
    .replace(/```[a-zA-Z]*/g, "") // strip ```lang fences (e.g. ```text)
    .replace(/```/g, "")
    .trim();
  let start = -1;
  for (let i = 0; i < text.length; i++) {
    const ch = text[i];
    if (ch === "[" || ch === "{") { start = i; break; }
  }
  if (start === -1) return null;
  const open = text[start];
  const close = open === "[" ? "]" : "}";
  let depth = 0;
  let inString = false;
  let escaped = false;
  for (let i = start; i < text.length; i++) {
    const ch = text[i];
    if (inString) {
      if (escaped) escaped = false;
      else if (ch === "\\") escaped = true;
      else if (ch === '"') inString = false;
      continue;
    }
    if (ch === '"') inString = true;
    else if (ch === "[" || ch === "{") depth++;
    else if (ch === "]" || ch === "}") {
      depth--;
      if (depth === 0) return text.slice(start, i + 1);
    }
  }
  return null;
}

// JSON strings cannot contain raw newlines/tabs — collapse any that the model
// emitted inside string values to a single space.
function repairJson(s: string): string {
  let out = "";
  let inString = false;
  let escaped = false;
  for (const ch of s) {
    if (inString) {
      if (escaped) { out += ch; escaped = false; }
      else if (ch === "\\") { out += ch; escaped = true; }
      else if (ch === '"') { out += ch; inString = false; }
      else if (ch === "\n" || ch === "\r" || ch === "\t") out += " ";
      else out += ch;
    } else {
      if (ch === '"') inString = true;
      out += ch;
    }
  }
  return out;
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  const authHeader = req.headers.get("Authorization");
  const userJwt = authHeader?.startsWith("Bearer ")
    ? authHeader.slice("Bearer ".length)
    : "";
  if (!userJwt) {
    return json(401, { success: false, error: "Missing bearer token" });
  }

  // Resolve the caller's uid from their own JWT.
  const { data: { user }, error: userError } = await admin.auth.getUser(userJwt);
  if (userError || !user) {
    return json(401, { success: false, error: "Invalid session" });
  }
  const userId = user.id;

  let contactId: string;
  try {
    const body = await req.json();
    contactId = body?.contactId;
  } catch {
    return json(400, { success: false, error: "Invalid request body" });
  }
  if (typeof contactId !== "string" || !contactId) {
    return json(400, { success: false, error: "contactId is required" });
  }

  // Ownership check (service role bypasses RLS; enforce it manually).
  const { data: contact, error: contactErr } = await admin
    .from("contacts")
    .select("id, name, check_in_frequency, last_check_in_date, next_check_in_date, streak_count")
    .eq("id", contactId)
    .eq("owner_id", userId)
    .maybeSingle();
  if (contactErr || !contact) {
    return json(404, { success: false, error: "Contact not found" });
  }

  // Cooldown: one brainstorm per contact per day.
  const todayUtc = new Date().toISOString().slice(0, 10);
  const { data: todaySession } = await admin
    .from("brainstorm_sessions")
    .select("id")
    .eq("contact_id", contactId)
    .gte("created_at", `${todayUtc}T00:00:00`)
    .limit(1);
  if (todaySession && todaySession.length > 0) {
    return json(200, { success: true, cooldown: true, topics: [] });
  }

  // Gather the contact's real context (notes + recent check-in notes + reminders).
  const { data: notesData } = await admin
    .from("notes")
    .select("title, body, created_at")
    .eq("contact_id", contactId)
    .eq("owner_id", userId)
    .order("created_at", { ascending: false });
  const { data: checkInsData } = await admin
    .from("check_ins")
    .select("note")
    .eq("contact_id", contactId)
    .not("note", "is", null)
    .order("checked_in_at", { ascending: false })
    .limit(5);
  const { data: remindersData } = await admin
    .from("custom_reminders")
    .select("title, description")
    .eq("contact_id", contactId)
    .eq("owner_id", userId)
    .limit(10);

  const notes = notesData ?? [];
  const checkIns = checkInsData ?? [];
  const reminders = remindersData ?? [];

  const hasContext = notes.length > 0 || checkIns.length > 0 || reminders.length > 0;

  let topics: { title: string; description: string | null; icon: string | null }[];
  if (!hasContext) {
    // No notes/relationship info yet — safe generic openers, no LLM call.
    topics = FALLBACK_TOPICS;
  } else {
    const prompt = buildPrompt(
      contact,
      (notes ?? []).map((n) => ({ title: n.title ?? "", body: n.body ?? null, created_at: n.created_at ?? "" })),
      (checkIns ?? []).map((c) => ({ note: c.note ?? null })),
      (reminders ?? []).map((r) => ({ title: r.title ?? "", description: r.description ?? null })),
    );
    const raw = await complete(prompt); // <-- LLM behind the abstraction
    const parsed = parseTopics(raw);
    // Resilient: if the model returns garbage, fall back to safe openers rather
    // than erroring on the user.
    topics = parsed && parsed.length ? parsed.slice(0, 6) : FALLBACK_TOPICS;
  }

  // Persist the session + topics (service role bypasses RLS; we already
  // confirmed the caller owns the contact above).
  const { data: session, error: sessionErr } = await admin
    .from("brainstorm_sessions")
    .insert({ contact_id: contactId })
    .select("id, created_at")
    .single();
  if (sessionErr || !session) {
    return json(500, { success: false, error: "Could not save session" });
  }

  const topicRows = topics.map((t) => ({
    session_id: session.id,
    icon: t.icon,
    title: t.title,
    description: t.description,
  }));
  const { data: insertedTopics, error: topicsErr } = await admin
    .from("brainstorm_topics")
    .insert(topicRows)
    .select("id, icon, title, description");
  if (topicsErr) {
    return json(500, { success: false, error: "Could not save topics" });
  }

  return json(200, {
    success: true,
    cooldown: false,
    session: { id: session.id, createdAt: session.created_at },
    topics: insertedTopics ?? [],
  });
});

