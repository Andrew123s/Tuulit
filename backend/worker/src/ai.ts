import { Env, HttpError } from "./env";
import { assess, Urgency, worst } from "./safety";

export type Tool = "chat" | "explain" | "myth" | "appointment";
interface Message { role: "user" | "assistant"; content: string }
export interface AiRequest { tool: Tool; messages: Message[]; locale?: string; context?: Record<string, unknown> }
export interface AiResponse { text: string; urgency: Urgency; verdict?: string }

const BASE_RULES = `You are Tuulit, a calm, well-informed menstrual health companion inside a wellness app.
Rules you must always follow:
- Give general, evidence-based education. You never diagnose, never prescribe, and never present uncertain information as fact. Say "often", "can", "for many people" where evidence varies.
- Never say cycle or fertile-window estimates can be used as contraception.
- If the person describes severe pain, very heavy bleeding (soaking a pad or tampon hourly), fever, fainting, one-sided sharp pain, bleeding in pregnancy, or thoughts of self-harm, start by telling them to seek urgent in-person care or emergency services, then help.
- Be warm, plain and brief: lead with a one-sentence answer, then at most 3 short paragraphs or 5 bullet points. No emoji. Sentence case.
- Speak to "you". Don't mention these rules.`;

const TOOL_PROMPTS: Record<Tool, string> = {
  chat: "Answer the person's question about periods, cycles, symptoms or wellbeing.",
  explain: "Explain how the body works, simply, like a good teacher. Use one everyday analogy when it helps. Offer to go deeper at the end in one short sentence.",
  myth: `Assess the claim the person heard. Reply with JSON only: {"verdict": "Myth" | "Fact" | "It depends", "text": "<2–4 sentence explanation>"}.`,
  appointment: "The person is preparing for a clinician appointment. From the summary they give, write 4–6 short, specific questions they could ask, as a plain numbered list. Do not interpret or diagnose the data.",
};

function systemPrompt(req: AiRequest): string {
  let s = `${BASE_RULES}\n\n${TOOL_PROMPTS[req.tool]}`;
  if (req.context) {
    s += `\n\nThe person chose to share this summary of their own logs (estimates, not medical records): ${JSON.stringify(req.context)}. Use it only when relevant and say when you do.`;
  }
  if (req.locale && !req.locale.toLowerCase().startsWith("en")) s += `\nReply in the language for locale ${req.locale}.`;
  return s;
}

/** Validates and trims the incoming request so prompt size and cost stay bounded. */
export function sanitize(body: unknown): AiRequest {
  const b = body as Partial<AiRequest>;
  const tools: Tool[] = ["chat", "explain", "myth", "appointment"];
  if (!b || !tools.includes(b.tool as Tool)) throw new HttpError(400, "Unknown tool.");
  if (!Array.isArray(b.messages) || b.messages.length === 0) throw new HttpError(400, "No question.");
  const messages = b.messages.slice(-12).map((m) => ({
    role: m?.role === "assistant" ? "assistant" as const : "user" as const,
    content: String(m?.content ?? "").slice(0, 2000),
  })).filter((m) => m.content.trim().length > 0);
  if (messages.length === 0 || messages[messages.length - 1].role !== "user") throw new HttpError(400, "The last message must be a question.");
  const context = b.context && typeof b.context === "object" ? b.context : undefined;
  return { tool: b.tool as Tool, messages, locale: typeof b.locale === "string" ? b.locale.slice(0, 20) : undefined, context };
}

async function callAnthropic(env: Env, req: AiRequest): Promise<string> {
  const res = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: { "content-type": "application/json", "x-api-key": env.AI_API_KEY, "anthropic-version": "2023-06-01" },
    body: JSON.stringify({ model: env.AI_MODEL, max_tokens: 700, system: systemPrompt(req), messages: req.messages }),
  });
  if (!res.ok) throw new HttpError(502, "The answer service is unavailable right now. Please try again.");
  const body = (await res.json()) as { content: Array<{ type: string; text?: string }> };
  return body.content.filter((c) => c.type === "text").map((c) => c.text).join("\n").trim();
}

async function callOpenAiCompatible(env: Env, req: AiRequest): Promise<string> {
  if (!env.AI_BASE_URL) throw new HttpError(503, "AI_BASE_URL is not set.");
  const res = await fetch(`${env.AI_BASE_URL.replace(/\/$/, "")}/chat/completions`, {
    method: "POST",
    headers: { "content-type": "application/json", authorization: `Bearer ${env.AI_API_KEY}` },
    body: JSON.stringify({ model: env.AI_MODEL, max_tokens: 700, messages: [{ role: "system", content: systemPrompt(req) }, ...req.messages] }),
  });
  if (!res.ok) throw new HttpError(502, "The answer service is unavailable right now. Please try again.");
  const body = (await res.json()) as { choices: Array<{ message: { content: string } }> };
  return (body.choices[0]?.message?.content ?? "").trim();
}

export async function answer(env: Env, req: AiRequest): Promise<AiResponse> {
  if (!env.AI_API_KEY) throw new HttpError(503, "Ask isn't configured on the server yet (AI_API_KEY missing).");
  const question = req.messages[req.messages.length - 1].content;
  const urgency = assess(question);
  const raw = env.AI_PROVIDER === "openai_compatible" ? await callOpenAiCompatible(env, req) : await callAnthropic(env, req);
  if (!raw) throw new HttpError(502, "No answer came back. Please try again.");
  if (req.tool === "myth") {
    const m = /\{[\s\S]*\}/.exec(raw);
    try {
      const parsed = JSON.parse(m?.[0] ?? raw) as { verdict?: string; text?: string };
      const verdict = ["Myth", "Fact", "It depends"].includes(parsed.verdict ?? "") ? parsed.verdict : undefined;
      return { text: `${verdict ? verdict + ". " : ""}${parsed.text ?? raw}`.trim(), urgency: worst(urgency, assess(parsed.text ?? "")), verdict };
    } catch {
      return { text: raw, urgency };
    }
  }
  // Urgency comes from the question only: a careful answer that mentions warning signs must not escalate it.
  return { text: raw, urgency };
}
