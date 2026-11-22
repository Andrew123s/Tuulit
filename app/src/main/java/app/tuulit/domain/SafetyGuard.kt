package app.tuulit.domain

/** How urgent a red flag is. EMERGENCY means "call emergency services now". */
enum class UrgencyLevel { NONE, SEE_CLINICIAN_SOON, URGENT, EMERGENCY }

data class SafetyAssessment(
    val level: UrgencyLevel,
    val headline: String,
    val message: String,
) {
    val isRedFlag: Boolean get() = level != UrgencyLevel.NONE
}

/**
 * Deterministic red-flag detector. It runs on the device before anything is sent to the AI,
 * and the Worker runs the same rules again server-side. It never diagnoses; it only routes
 * people towards in-person care when their words match known warning signs.
 */
object MedicalSafetyGuard {

    private data class Rule(val level: UrgencyLevel, val patterns: List<Regex>)

    private fun r(vararg p: String) = p.map { Regex(it, RegexOption.IGNORE_CASE) }

    private val rules = listOf(
        Rule(
            UrgencyLevel.EMERGENCY,
            r(
                """\b(suicid\w*|kill (my|him|her)self|end my life|hurting myself|self[- ]?harm)\b""",
                """\b(chest pain|can'?t breathe|shortness of breath|trouble breathing)\b""",
                """\b(faint(ed|ing)?|pass(ed|ing) out|lost consciousness)\b""",
                """\bpregnan\w*\b.*\b(bleed\w*|pain)\b|\b(bleed\w*|pain)\b.*\bpregnan\w*\b""",
                """\bsharp\b.*\bone side\b|\bone[- ]sided\b.*\bpain\b""",
            ),
        ),
        Rule(
            UrgencyLevel.URGENT,
            r(
                """\b(soak(ing|ed)? through|pad every hour|tampon every hour|every hour)\b""",
                """\bfever\b|\b(10[1-4]|3[89]([.,]\d)?) ?(°|degrees)?\s?(f|c)\b""",
                """\b(worst pain|unbearable|sudden severe|can'?t (stand|walk))\b""",
                """\bvomit\w*\b.*\b(non ?stop|can'?t stop|all day)\b""",
                """\bclots?\b.*\b(large|huge|golf|plum)\b""",
            ),
        ),
        Rule(
            UrgencyLevel.SEE_CLINICIAN_SOON,
            r(
                """\b(no period|missed (my )?period)\b.*\b(3|three|4|four|5|five|6|six) months\b""",
                """\bbleed\w*\b.*\b(after sex|between periods|after menopause)\b""",
            ),
        ),
    )

    fun assess(input: String): SafetyAssessment {
        val text = input.trim()
        if (text.isEmpty()) return none
        val level = rules.firstOrNull { rule -> rule.patterns.any { it.containsMatchIn(text) } }?.level
            ?: return none
        return when (level) {
            UrgencyLevel.EMERGENCY -> SafetyAssessment(
                level,
                "Please get medical help now",
                "What you describe can be serious. Call your local emergency number or go to urgent care. " +
                    "If you are thinking about harming yourself, contact a crisis line or emergency services right away.",
            )
            UrgencyLevel.URGENT -> SafetyAssessment(
                level,
                "Please see a doctor today",
                "Very heavy bleeding, fever, or severe pain that stops you from moving deserves in-person care today.",
            )
            UrgencyLevel.SEE_CLINICIAN_SOON -> SafetyAssessment(
                level,
                "Worth booking an appointment",
                "This is worth checking with a clinician soon. Appointment prep in Ask can help you get ready.",
            )
            UrgencyLevel.NONE -> none
        }
    }

    /** Compatibility helper: returns the warning text when anything urgent or emergency is found. */
    fun inspectForUrgentWarning(input: String): String? {
        val a = assess(input)
        return if (a.level == UrgencyLevel.URGENT || a.level == UrgencyLevel.EMERGENCY) "${a.headline}. ${a.message}" else null
    }

    private val none = SafetyAssessment(UrgencyLevel.NONE, "", "")
}
