/* Server-side copy of the app's MedicalSafetyGuard. The Worker re-checks every question so the
   safety notice never depends on the client alone. Keep in step with SafetyGuard.kt. */

export type Urgency = "NONE" | "SEE_CLINICIAN_SOON" | "URGENT" | "EMERGENCY";

const rules: Array<[Urgency, RegExp[]]> = [
  ["EMERGENCY", [
    /\b(suicid\w*|kill (my|him|her)self|end my life|hurting myself|self[- ]?harm)\b/i,
    /\b(chest pain|can'?t breathe|shortness of breath|trouble breathing)\b/i,
    /\b(faint(ed|ing)?|pass(ed|ing) out|lost consciousness)\b/i,
    /\bpregnan\w*\b.*\b(bleed\w*|pain)\b|\b(bleed\w*|pain)\b.*\bpregnan\w*\b/i,
    /\bsharp\b.*\bone side\b|\bone[- ]sided\b.*\bpain\b/i,
  ]],
  ["URGENT", [
    /\b(soak(ing|ed)? through|pad every hour|tampon every hour|every hour)\b/i,
    /\bfever\b|\b(10[1-4]|3[89]([.,]\d)?) ?(°|degrees)?\s?(f|c)\b/i,
    /\b(worst pain|unbearable|sudden severe|can'?t (stand|walk))\b/i,
    /\bvomit\w*\b.*\b(non ?stop|can'?t stop|all day)\b/i,
    /\bclots?\b.*\b(large|huge|golf|plum)\b/i,
  ]],
  ["SEE_CLINICIAN_SOON", [
    /\b(no period|missed (my )?period)\b.*\b(3|three|4|four|5|five|6|six) months\b/i,
    /\bbleed\w*\b.*\b(after sex|between periods|after menopause)\b/i,
  ]],
];

export function assess(text: string): Urgency {
  const t = text.trim();
  if (!t) return "NONE";
  for (const [level, patterns] of rules) if (patterns.some((p) => p.test(t))) return level;
  return "NONE";
}

export const worst = (a: Urgency, b: Urgency): Urgency => {
  const order: Urgency[] = ["NONE", "SEE_CLINICIAN_SOON", "URGENT", "EMERGENCY"];
  return order.indexOf(a) >= order.indexOf(b) ? a : b;
};
