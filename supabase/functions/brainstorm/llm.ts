// =============================================================================
// Foster — Brainstorm LLM abstraction
// -----------------------------------------------------------------------------
// This is the ONLY file that talks to the LLM provider. Everything else in
// index.ts calls `complete(prompt)` and expects plain text back.
//
// To swap providers or move to a paid tier later, change ONLY:
//   1. MODEL_NAME (and the endpoint URL if the provider changes)
//   2. The request/response handling inside complete()
// Nothing in the rest of the feature needs to change.
// =============================================================================

export const MODEL_NAME = "gemini-2.5-flash"; // e.g. "gemini-3.5-flash" when GA / key allows

export async function complete(prompt: string): Promise<string> {
  const apiKey = Deno.env.get("GEMINI_API_KEY");
  if (!apiKey) {
    throw new Error("GEMINI_API_KEY is not configured");
  }
  const url =
    `https://generativelanguage.googleapis.com/v1beta/models/${MODEL_NAME}:generateContent?key=${apiKey}`;

  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      contents: [{ parts: [{ text: prompt }] }],
      generationConfig: {
        temperature: 0.9,
        maxOutputTokens: 1024,
        // Disable chain-of-thought: the suggestions are short, and "thinking"
        // adds several seconds of latency that can exceed both the Supabase
        // function timeout and the client request timeout (-> "network error"),
        // and it was the source of the thinking-in-parts parsing problem.
        thinkingConfig: { thinkingBudget: 0 },
      },
    }),
  });

  if (!res.ok) {
    const detail = await res.text().catch(() => "");
    throw new Error(`Gemini ${res.status}: ${detail.slice(0, 300)}`);
  }

  const data = await res.json();
  const parts: { text?: string; thought?: boolean }[] =
    data?.candidates?.[0]?.content?.parts ?? [];

  // Gemini 2.5-class models put chain-of-thought in parts with `thought: true`
  // (sometimes as parts[0]) and the real answer in the remaining parts. Joining
  // only the non-thought parts gets the actual final answer.
  const answerParts = parts.filter((p) => !p.thought);
  const source = (answerParts.length ? answerParts : parts)
    .map((p) => p.text ?? "")
    .join("");
  const text = source.trim();

  if (!text) {
    throw new Error("Empty LLM response");
  }
  return text;
}
