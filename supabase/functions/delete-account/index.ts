// =============================================================================
// Foster — delete-account Edge Function
// -----------------------------------------------------------------------------
// Deleting a user from `auth.users` requires the Admin API (service role). A
// `security definer` Postgres function CANNOT do it: its owner role (`postgres`)
// has no DELETE privilege on `auth.users` — only `supabase_auth_admin` does.
// So the client calls THIS function (with the user's own JWT), the function
// verifies the caller, then deletes via `supabase.auth.admin.deleteUser()`.
//
// Because every public table (profiles, groups, custom_reminders, notes,
// notification_settings, contacts, user_badges, subscriptions) FK-cascades to
// auth.users (and contact_groups/check_ins/brainstorm_sessions/brainstorm_topics
// cascade via contacts), deleting the auth.users row removes everything.
//
// Deploy:
//   supabase functions deploy delete-account --no-verify-jwt
//   (service role access comes from the function's own environment, never the client)
// =============================================================================

import { createClient } from "npm:@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

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

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    return json(401, { success: false, error: "Missing bearer token" });
  }
  const userJwt = authHeader.slice("Bearer ".length);

  const adminClient = createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false },
  });

  // Verify the caller's session and resolve their uid from the JWT.
  const { data: { user }, error: userError } =
    await adminClient.auth.getUser(userJwt);
  if (userError || !user) {
    return json(401, { success: false, error: "Invalid session" });
  }
  const userId = user.id;

  // Best-effort: remove the user's avatar folder under avatars/{userId}/.
  // Ignore failures here so storage issues never block the actual deletion.
  try {
    const { data: files } = await adminClient.storage
      .from("avatars")
      .list(userId);
    if (files?.length) {
      await adminClient.storage
        .from("avatars")
        .remove(files.map((f) => `${userId}/${f.name}`));
    }
  } catch {
    // no-op
  }

  // Delete the auth.users row. Every related public-table row cascades away.
  const { error: deleteError } = await adminClient.auth.admin.deleteUser(
    userId,
  );
  if (deleteError) {
    return json(500, { success: false, error: deleteError.message });
  }

  return json(200, { success: true });
});
