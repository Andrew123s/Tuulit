package app.tuulit.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.tuulit.domain.Achievement
import app.tuulit.domain.PlayContent
import app.tuulit.ui.FixedScreen
import app.tuulit.ui.container
import app.tuulit.ui.designsystem.ButtonKind
import app.tuulit.ui.designsystem.GroupTitle
import app.tuulit.ui.designsystem.IconBtn
import app.tuulit.ui.designsystem.ListRow
import app.tuulit.ui.designsystem.Mascot
import app.tuulit.ui.designsystem.MascotMood
import app.tuulit.ui.designsystem.Overline
import app.tuulit.ui.designsystem.Panel
import app.tuulit.ui.designsystem.Progress
import app.tuulit.ui.designsystem.T
import app.tuulit.ui.designsystem.TButton
import app.tuulit.ui.designsystem.Tuulit
import app.tuulit.ui.designsystem.TuulitIcons
import kotlinx.coroutines.launch

private enum class AnswerState { IDLE, RIGHT, WRONG, DIM, CHOSEN }

@Composable
fun QuizScreen(id: String, onBack: () -> Unit, go: (String) -> Unit) {
    val quiz = PlayContent.quizzes.firstOrNull { it.id == id } ?: run { onBack(); return }
    val c = container()
    val scope = rememberCoroutineScope()
    var index by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<Int?>(null) }
    var correct by remember { mutableIntStateOf(0) }
    val missed = remember { mutableListOf<String>() }
    var finished by remember { mutableStateOf(false) }
    LaunchedEffect(finished) { if (finished) { c.carePlay.unlock(Achievement.FIRST_QUIZ); c.carePlay.recordGame("quiz_${quiz.id}", correct) } }

    FixedScreen {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(TuulitIcons.Close, "Close quiz", onBack)
            if (!finished) Progress(quiz.questions.size, index + 1, Modifier.weight(1f).padding(start = 12.dp, end = 56.dp))
        }
        if (finished) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Mascot(MascotMood.HAPPY, 96.dp, Modifier.padding(top = 8.dp))
                if (quiz.isReflective) {
                    T("Thanks for checking in.", Tuulit.type.display, modifier = Modifier.padding(top = 12.dp))
                    T("Whatever you picked is valid. Add what you need to your Comfort Library so it's one tap away.", Tuulit.type.body, Tuulit.colors.inkMuted, Modifier.padding(top = 8.dp))
                } else {
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 12.dp)) {
                        T("$correct", Tuulit.type.numeral); T(" of ${quiz.questions.size}", Tuulit.type.display, Tuulit.colors.inkMuted)
                    }
                    T(when { correct == quiz.questions.size -> "You know your stuff."; correct * 2 >= quiz.questions.size -> "Nicely done."; else -> "Now you know a bit more." },
                        Tuulit.type.headline, modifier = Modifier.padding(top = 6.dp))
                    GroupTitle("Worth remembering", Modifier.padding(top = 20.dp))
                    quiz.questions.forEachIndexed { i, q -> ListRow(q.explanation, icon = TuulitIcons.Info, last = i == quiz.questions.lastIndex) }
                }
            }
            if (quiz.forPartner || quiz.id == "myths_facts") TButton("Send the couple quiz to my person", { go("connect") }, Modifier.fillMaxWidth().padding(bottom = 10.dp),
                kind = ButtonKind.QUIET, icon = TuulitIcons.Share)
            if (quiz.isReflective) TButton("Open Comfort Library", { onBack(); go("care/library") }, Modifier.fillMaxWidth().padding(bottom = 10.dp), kind = ButtonKind.QUIET)
            TButton("Done", onBack, Modifier.fillMaxWidth())
            return@FixedScreen
        }
        val q = quiz.questions[index]
        val answered = chosen != null
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Overline("${quiz.title} · ${index + 1} of ${quiz.questions.size}", modifier = Modifier.padding(top = 22.dp))
            T(if (q.isStatement) "“${q.prompt}”" else q.prompt, if (q.isStatement) Tuulit.type.display else Tuulit.type.headline,
                if (answered && q.isStatement) Tuulit.colors.inkMuted else Tuulit.colors.ink, Modifier.padding(top = 10.dp))
            if (q.correctIndex < 0) T("There's no right answer.", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 8.dp))
        }
        if (answered) {
            val right = q.correctIndex < 0 || chosen == q.correctIndex
            Panel(Modifier.padding(bottom = 16.dp), padding = 18.dp) {
                T(when {
                    q.correctIndex < 0 -> "Noted."
                    right -> if (q.isStatement) "That's right, it's a ${q.options[q.correctIndex].lowercase()}." else "That's right."
                    else -> "Not quite. It's ${q.options[q.correctIndex].let { if (q.isStatement) "a ${it.lowercase()}" else it }}."
                }, Tuulit.type.title)
                T(q.explanation, Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 6.dp))
            }
        }
        q.options.forEachIndexed { i, o ->
            val state = when {
                !answered -> AnswerState.IDLE
                q.correctIndex < 0 -> if (i == chosen) AnswerState.CHOSEN else AnswerState.IDLE
                i == q.correctIndex -> AnswerState.RIGHT
                i == chosen -> AnswerState.WRONG
                else -> AnswerState.DIM
            }
            Answer(o, state, enabled = !answered) {
                chosen = i
                if (q.correctIndex >= 0 && i == q.correctIndex) correct++
                else if (q.correctIndex >= 0) missed += q.explanation
            }
        }
        if (answered) TButton(if (index < quiz.questions.lastIndex) "Next" else "See result", {
            if (index < quiz.questions.lastIndex) { index++; chosen = null } else finished = true
        }, Modifier.fillMaxWidth().padding(top = 16.dp))
    }
}

@Composable
private fun Answer(text: String, state: AnswerState, enabled: Boolean, onClick: () -> Unit) {
    val c = Tuulit.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 60.dp).clip(shape)
            .background(when (state) { AnswerState.RIGHT, AnswerState.CHOSEN -> c.ink; AnswerState.WRONG -> c.cream; else -> c.sand })
            .then(if (state == AnswerState.WRONG) Modifier.border(1.5.dp, c.inkFaint, shape) else Modifier)
            .alpha(if (state == AnswerState.DIM) 0.45f else 1f)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { stateDescription = when (state) { AnswerState.RIGHT -> "Correct answer"; AnswerState.WRONG -> "Your answer, not correct"; AnswerState.CHOSEN -> "Chosen"; else -> "" } }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        T(text, Tuulit.type.label.copy(fontSize = Tuulit.type.body.fontSize),
            when (state) { AnswerState.RIGHT, AnswerState.CHOSEN -> c.onInk; AnswerState.WRONG -> c.inkMuted; else -> c.ink }, Modifier.weight(1f))
        when (state) {
            AnswerState.RIGHT, AnswerState.CHOSEN -> Icon(TuulitIcons.Check, null, Modifier.size(20.dp), tint = c.onInk)
            AnswerState.WRONG -> Icon(TuulitIcons.Close, null, Modifier.size(18.dp), tint = c.inkMuted)
            else -> Box(Modifier.size(0.dp))
        }
    }
}
