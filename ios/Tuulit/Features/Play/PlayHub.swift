import SwiftUI
import TuulitCore

/// Play hub inside Care: featured game, today's pick, real progress, the game grid with artwork,
/// quizzes and earned moments. Scores are personal bests; there are no streaks.
struct PlayHub: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c

    var body: some View {
        let scores = model.carePlay.gameScores
        let achievements = model.carePlay.achievements
        let featured = PlayContent.game("banana_catch")!
        let daily = PlayContent.dailyPick(today: model.snapshot.today)
        let plays = scores.values.reduce(0) { $0 + $1.plays }
        let look = c.gameLook(featured.id)
        let _ = model.dataVersion
        VStack(alignment: .leading, spacing: 0) {
            Button { model.push(.game(featured.id)) } label: {
                ZStack(alignment: .leading) {
                    look.bg
                    GameArt(id: featured.id).frame(width: 170, height: 170).frame(maxWidth: .infinity, alignment: .trailing)
                    VStack(alignment: .leading, spacing: 4) {
                        Overline("Featured game", look.ink)
                        T(featured.title, .display, look.ink)
                        T(featured.blurb, .bodySm, look.ink)
                        if let best = scores[featured.id]?.best, best > 0 { T("Your best: \(best)", .label, look.ink).padding(.top, 4) }
                        T("Play now", .label, Palette.light.onInk).padding(.horizontal, 22).frame(minHeight: 48)
                            .background(Palette.light.ink, in: Capsule()).padding(.top, 10)
                    }
                    .padding(.leading, 22).padding(.vertical, 22).padding(.trailing, 150)
                }
                .clipShape(RoundedRectangle(cornerRadius: Radius.xl, style: .continuous))
            }
            .buttonStyle(PressScale())
            .accessibilityLabel("Featured game, \(featured.title). \(featured.blurb)")

            HStack(spacing: 12) {
                Button { model.push(.game(daily.id)) } label: {
                    let lk = c.gameLook(daily.id)
                    let best = scores[daily.id]?.best ?? 0
                    VStack(alignment: .leading, spacing: 4) {
                        Overline("Today's pick", lk.ink)
                        T(daily.title, .title, lk.ink)
                        T(best > 0 ? "Beat your best of \(best)" : "Give it a first try", .caption, lk.ink)
                        Spacer(minLength: 0)
                    }
                    .padding(16).frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    .background(lk.bg, in: RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
                }
                .buttonStyle(PressScale())
                .layoutPriority(1.2)
                VStack(alignment: .leading, spacing: 2) {
                    T("\(plays)", .display)
                    T(plays == 1 ? "game played" : "games played", .caption, c.inkMuted)
                    T("\(achievements.count) moments", .label, c.toffee).padding(.top, 6)
                    Spacer(minLength: 0)
                }
                .padding(16).frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                .background(c.oat, in: RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
                .accessibilityElement(children: .combine)
            }
            .fixedSize(horizontal: false, vertical: true)
            .padding(.top, 14)

            T("All games", .title).padding(.top, 26).padding(.bottom, 10)
            LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 12) {
                ForEach(PlayContent.games.filter { $0.id != featured.id }) { g in
                    Button { model.push(.game(g.id)) } label: {
                        VStack(alignment: .leading, spacing: 0) {
                            GameArt(id: g.id).frame(height: 110).frame(maxWidth: .infinity)
                                .background(c.gameLook(g.id).bg, in: RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
                            VStack(alignment: .leading, spacing: 2) {
                                T(g.title, .subtitle, lines: 1)
                                let best = scores[g.id]?.best ?? 0
                                T(best > 0 ? "\(g.kind) · best \(best)" : g.kind, .caption, c.inkMuted)
                            }
                            .padding(.horizontal, 14).padding(.top, 10).padding(.bottom, 14)
                        }
                        .background(c.oat, in: RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
                    }
                    .buttonStyle(PressScale())
                    .accessibilityLabel("\(g.title), \(g.kind)")
                    .accessibilityIdentifier("game.\(g.id)")
                }
            }

            T("Quizzes", .title).padding(.top, 14).padding(.bottom, 10)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    let tints = [c.roseSoft, c.fertileSoft, c.peachSoft, c.bananaSoft]
                    ForEach(Array(PlayContent.quizzes.enumerated()), id: \.element.id) { i, q in
                        Button { model.push(.quiz(q.id)) } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                TIcon(Icons.named(q.icon)).frame(width: 44, height: 44).background(c.oat, in: Circle())
                                T(q.title, .title, lines: 2).padding(.top, 8)
                                T(q.subtitle, .caption, c.inkMuted, lines: 2)
                                Spacer(minLength: 0)
                            }
                            .padding(18).frame(width: 200, height: 196, alignment: .topLeading)
                            .background(tints[i % tints.count], in: RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
                        }
                        .buttonStyle(PressScale())
                    }
                }
            }

            if !achievements.isEmpty {
                T("Moments", .title).padding(.top, 24).padding(.bottom, 10)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(achievements, id: \.achievement) { a in
                            VStack(spacing: 6) {
                                Illustration(kind: .spark, size: 64, bg: c.bananaSoft)
                                T(a.achievement.title, .caption, align: .center, lines: 2)
                            }
                            .frame(width: 120)
                            .accessibilityElement(children: .combine)
                            .accessibilityHint(a.achievement.description)
                        }
                    }
                }
            }
            T("Scores are personal bests. There are no streaks to keep.", .caption, c.inkFaint).padding(.top, 16)
        }
    }
}

/// Quiz player: myths or facts, knowledge, reflective check-ins and the couple quiz.
struct QuizView: View {
    let id: String
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var index = 0
    @State private var chosen: Int?
    @State private var correct = 0
    @State private var finished = false

    var body: some View {
        if let quiz = PlayContent.quiz(id) { content(quiz) } else { EmptyState(title: "Quiz not found", message: "") }
    }

    private func content(_ quiz: Quiz) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                IconBtn(Icons.close, "Close quiz") { model.pop() }
                if !finished { StepProgress(steps: quiz.questions.count, done: index + 1).padding(.leading, 12).padding(.trailing, 56) }
            }
            .padding(.top, 8)
            if finished { result(quiz) } else { question(quiz) }
        }
        .padding(.horizontal, 20).padding(.bottom, 20)
        .background(c.cream.ignoresSafeArea())
        .onChange(of: finished) { _, f in
            if f { model.carePlay.unlock(.FIRST_QUIZ); model.carePlay.recordGame("quiz_\(quiz.id)", score: correct) }
        }
    }

    @ViewBuilder private func result(_ quiz: Quiz) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Mascot(.happy, size: 96).padding(.top, 8)
                if quiz.isReflective {
                    T("Thanks for checking in.", .display).padding(.top, 12)
                    T("Whatever you picked is valid. Add what you need to your Comfort Library so it's one tap away.", .body, c.inkMuted).padding(.top, 8)
                } else {
                    HStack(alignment: .lastTextBaseline, spacing: 0) {
                        T("\(correct)", .numeral); T(" of \(quiz.questions.count)", .display, c.inkMuted)
                    }
                    .padding(.top, 12)
                    T(correct == quiz.questions.count ? "You know your stuff." : correct * 2 >= quiz.questions.count ? "Nicely done." : "Now you know a bit more.", .headline)
                        .padding(.top, 6)
                    GroupTitle("Worth remembering").padding(.top, 20)
                    ForEach(Array(quiz.questions.enumerated()), id: \.offset) { i, q in ListRow(q.explanation, icon: Icons.info, last: i == quiz.questions.count - 1) }
                }
            }
        }
        if quiz.forPartner || quiz.id == "myths_facts" {
            TButton("Send the couple quiz to my person", kind: .quiet, icon: Icons.share, fullWidth: true) { model.push(.connect) }.padding(.bottom, 10)
        }
        if quiz.isReflective { TButton("Open Comfort Library", kind: .quiet, fullWidth: true) { model.pop(); model.go("care/library") }.padding(.bottom, 10) }
        TButton("Done", fullWidth: true) { model.pop() }
    }

    @ViewBuilder private func question(_ quiz: Quiz) -> some View {
        let q = quiz.questions[index]
        let answered = chosen != nil
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Overline("\(quiz.title) · \(index + 1) of \(quiz.questions.count)").padding(.top, 22)
                T(q.isStatement ? "“\(q.prompt)”" : q.prompt, q.isStatement ? .display : .headline, answered && q.isStatement ? c.inkMuted : c.ink).padding(.top, 10)
                if q.correctIndex < 0 { T("There's no right answer.", .bodySm, c.inkMuted).padding(.top, 8) }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        if answered {
            let right = q.correctIndex < 0 || chosen == q.correctIndex
            Panel(padding: 18) {
                T(q.correctIndex < 0 ? "Noted." : right ? (q.isStatement ? "That's right, it's a \(q.options[q.correctIndex].lowercased())." : "That's right.")
                  : "Not quite. It's \(q.isStatement ? "a " + q.options[q.correctIndex].lowercased() : q.options[q.correctIndex]).", .title)
                T(q.explanation, .bodySm, c.inkMuted).padding(.top, 6)
            }
            .padding(.bottom, 16)
            .accessibilityElement(children: .combine)
        }
        ForEach(Array(q.options.enumerated()), id: \.offset) { i, o in
            let state: AnswerState = !answered ? .idle : q.correctIndex < 0 ? (i == chosen ? .chosen : .idle)
                : i == q.correctIndex ? .right : i == chosen ? .wrong : .dim
            AnswerRow(text: o, state: state, enabled: !answered) {
                chosen = i
                if q.correctIndex >= 0 && i == q.correctIndex { correct += 1 }
            }
        }
        if answered {
            TButton(index < quiz.questions.count - 1 ? "Next" : "See result", fullWidth: true) {
                if index < quiz.questions.count - 1 { index += 1; chosen = nil } else { finished = true }
            }
            .padding(.top, 16)
        }
    }
}

private enum AnswerState { case idle, right, wrong, dim, chosen }

private struct AnswerRow: View {
    let text: String
    let state: AnswerState
    let enabled: Bool
    let action: () -> Void
    @Environment(\.palette) private var c
    var body: some View {
        Button(action: action) {
            HStack {
                T(text, .body, state == .wrong ? c.inkMuted : c.ink).fontWeight(.semibold)
                Spacer()
                if state == .right || state == .chosen { TIcon(Icons.check, size: 20, color: c.lilacDeep) }
                if state == .wrong { TIcon(Icons.close, size: 18, color: c.inkMuted) }
            }
            .padding(.horizontal, 18).padding(.vertical, 14).frame(minHeight: 60)
            .background(state == .right || state == .chosen ? c.fertileSoft : state == .wrong ? c.cream : c.sand,
                        in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(state == .right || state == .chosen ? c.fertile : state == .wrong ? c.inkFaint : .clear,
                                                               lineWidth: state == .wrong ? 1.5 : 2))
            .opacity(state == .dim ? 0.45 : 1)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .padding(.top, 10)
        .accessibilityValue(state == .right ? "Correct answer" : state == .wrong ? "Your answer, not correct" : state == .chosen ? "Chosen" : "")
        .sensoryFeedback(state == .wrong ? .warning : .success, trigger: state == .right || state == .wrong)
    }
}
