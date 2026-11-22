package app.tuulit.domain

data class QuizQuestion(
    val prompt: String,
    val options: List<String>,
    /** Index of the right answer, or -1 for reflective questions with no right answer. */
    val correctIndex: Int,
    val explanation: String,
    val isStatement: Boolean = false,
)

data class Quiz(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val questions: List<QuizQuestion>,
    val forPartner: Boolean = false,
) {
    val isReflective: Boolean get() = questions.all { it.correctIndex < 0 }
}

data class GameInfo(val id: String, val title: String, val kind: String, val icon: String, val blurb: String)

object PlayContent {

    val games = listOf(
        GameInfo("banana_catch", "Banana Catch", "Arcade", "play", "Catch bananas, dodge rocks, three lives."),
        GameInfo("mood_match", "Mood Match", "Memory", "memory", "Find the matching pairs of comfort symbols."),
        GameInfo("memory_flip", "Memory Flip", "Sequence", "memory", "Watch the tiles light up, then repeat the order."),
        GameInfo("brain_break", "Brain Break", "Quick sums", "quiz", "Gentle mental arithmetic against a soft timer."),
        GameInfo("word_puzzle", "Word Puzzle", "Words", "book", "Unscramble calm, cosy words."),
        GameInfo("calm_tap", "Calm Tap", "Rhythm", "breath", "Tap as the circle reaches the ring."),
        GameInfo("focus_game", "Focus Game", "Attention", "eye", "Tap only the banana among the shapes."),
        GameInfo("stress_smash", "Stress Smash", "Let it out", "pain", "Crush tiles. No timer, no losing."),
    )

    fun game(id: String) = games.first { it.id == id }

    /** Word Puzzle answers with a gentle hint each. All uppercase, 4–7 letters. */
    val words = listOf(
        "CALM" to "How you feel after slow breathing",
        "COSY" to "Blanket, tea and rain outside",
        "REST" to "Valid on any day",
        "WARMTH" to "What a hot water bottle gives",
        "GINGER" to "A tea that can settle cramps",
        "BREATHE" to "In for four, out for six",
        "SLEEP" to "What a wind-down helps with",
        "BANANA" to "Tuulit's favourite fruit",
        "GENTLE" to "How to treat yourself today",
        "STRETCH" to "Cat and cow is one",
        "COMFORT" to "What Care is for",
        "PEACE" to "Quiet in your mind",
    )

    val quizzes = listOf(
        Quiz(
            "myths_facts", "Period myths or facts?", "4 statements, no judgement", "quiz",
            listOf(
                QuizQuestion("Everyone with a healthy cycle has a 28-day cycle.", listOf("Fact", "Myth"), 1,
                    "Healthy adult cycles usually run 21 to 35 days, and they can shift by a few days from month to month.", isStatement = true),
                QuizQuestion("Pain that stops you working or sleeping is just something to put up with.", listOf("Fact", "Myth"), 1,
                    "Disruptive pain deserves a clinician's attention. It can have treatable causes such as endometriosis.", isStatement = true),
                QuizQuestion("Gentle movement during your period is unsafe.", listOf("Fact", "Myth"), 1,
                    "Gentle movement is safe if it feels comfortable, and rest is equally valid.", isStatement = true),
                QuizQuestion("Calendar estimates of your fertile window are reliable contraception.", listOf("Fact", "Myth"), 1,
                    "Ovulation timing shifts easily. Calendar estimates should never be used as birth control.", isStatement = true),
            ),
        ),
        Quiz(
            "cycle_knowledge", "Know your cycle", "3 questions on how your body works", "book",
            listOf(
                QuizQuestion("Which chemical messengers trigger period cramps?", listOf("Prostaglandins", "Melatonin", "Adrenaline"), 0,
                    "Prostaglandins make the uterus contract to shed its lining. Higher levels often mean stronger cramps."),
                QuizQuestion("Which phase usually comes between ovulation and your next period?", listOf("Follicular phase", "Luteal phase", "Menstrual phase"), 1,
                    "The luteal phase follows ovulation and lasts roughly 11 to 16 days for most people."),
                QuizQuestion("Can stress, travel or illness shift when your period arrives?", listOf("Yes, they can", "No, never"), 0,
                    "The brain and ovaries talk through hormones that respond to stress, illness and sleep changes."),
            ),
        ),
        Quiz(
            "self_care", "What do you need today?", "A check-in with no right answers", "care",
            listOf(
                QuizQuestion("How does your social battery feel right now?",
                    listOf("Quiet blanket mode", "A cosy one-on-one chat", "Up for a distraction"), -1,
                    "Honour your real capacity today. There is no right answer."),
                QuizQuestion("Which comfort sounds best in the next 10 minutes?",
                    listOf("Warm drink and breathing", "A quick game", "Phone down and rest"), -1,
                    "Add your favourite to your Comfort Library so it's one tap away next time."),
            ),
        ),
        Quiz(
            "couple_support", "Couple quiz", "How well does your person know periods?", "users",
            listOf(
                QuizQuestion("Your person says their cramps are rough today. What's the best first move?",
                    listOf("Ask what would help right now", "Give medical advice", "Say it can't be that bad"), 0,
                    "Asking rather than assuming respects their comfort and builds trust."),
                QuizQuestion("Are two people's cycles and symptoms basically the same?",
                    listOf("Yes, mostly", "No, they vary widely"), 1,
                    "Symptoms, flow, energy and cycle length differ a lot from person to person."),
                QuizQuestion("If your partner is frustrated in their luteal phase, should you blame hormones?",
                    listOf("No, listen to the actual concern first", "Yes, mention their cycle"), 0,
                    "Dismissing real feelings as 'just hormones' is invalidating. Listen first."),
            ),
            forPartner = true,
        ),
    )

    fun quiz(id: String) = quizzes.first { it.id == id }
}

/** Achievements are earned by real actions. Never streaks, never loss. */
enum class Achievement(val title: String, val description: String) {
    FIRST_LOG("First check-in", "You logged how you felt for the first time."),
    FIRST_PERIOD("Cycle started", "You logged your first period in Tuulit."),
    FIRST_SESSION("Took a moment", "You finished your first Care session."),
    FIVE_SESSIONS("Comfort habit", "You finished five Care sessions."),
    FIRST_GAME("Two minutes off", "You finished your first game."),
    FIRST_QUIZ("Curious mind", "You completed a quiz."),
    INSIGHTS_EXPLORED("Pattern spotter", "You explored your insights."),
    PAIN_JOURNAL("Keeping track", "You recorded a pain journal entry."),
    CONNECTED("Let someone in", "You connected with a trusted person."),
}
