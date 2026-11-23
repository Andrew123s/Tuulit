import SwiftUI
import TuulitCore

/// Hosts a game: tinted header with art and live score, the game itself, and the result sheet
/// with personal best and "Play again". Mechanics and scoring match Android `Games.kt`.
struct GameView: View {
    let id: String
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var round = 0
    @State private var best = 0
    @State private var live = 0
    @State private var result: (score: Int, line: String, newBest: Bool)?

    var body: some View {
        if let info = PlayContent.game(id) { content(info) } else { EmptyState(title: "Game not found", message: "") }
    }

    private func finish(_ score: Int, _ line: String) {
        guard result == nil else { return }
        let newBest = model.carePlay.recordGame(id, score: score)
        result = (score, line, newBest)
    }

    private func content(_ info: GameInfo) -> some View {
        let look = c.gameLook(id)
        return VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .leading) {
                look.bg
                GameArt(id: id).frame(width: 110, height: 110).frame(maxWidth: .infinity, alignment: .trailing).padding(.trailing, 60).accessibilityHidden(true)
                HStack {
                    IconBtn(Icons.close, "Close game") { model.pop() }
                    VStack(alignment: .leading, spacing: 0) {
                        T(info.title, .title, look.ink)
                        T(best > 0 ? "Best \(best)" : info.kind, .caption, look.ink)
                    }
                    .padding(.leading, 12)
                    Spacer()
                    T("\(live)", .headline).frame(width: 58, height: 58).background(c.oat, in: Circle()).accessibilityLabel("Score \(live)")
                }
                .padding(14)
            }
            .clipShape(RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
            .padding(.top, 8).padding(.bottom, 12)
            T(info.blurb, .caption, c.inkMuted).padding(.bottom, 10)
            Group {
                switch id {
                case "banana_catch": BananaCatch(onScore: { live = $0 }, onEnd: finish)
                case "mood_match": MoodMatch(onScore: { live = $0 }, onEnd: finish)
                case "memory_flip": MemoryFlip(onScore: { live = $0 }, onEnd: finish)
                case "brain_break": BrainBreak(onScore: { live = $0 }, onEnd: finish)
                case "word_puzzle": WordPuzzle(onScore: { live = $0 }, onEnd: finish)
                case "calm_tap": CalmTap(onScore: { live = $0 }, onEnd: finish)
                case "focus_game": FocusGame(onScore: { live = $0 }, onEnd: finish)
                default: StressSmash(onScore: { live = $0 }, onEnd: finish)
                }
            }
            .id(round)
            .frame(maxHeight: .infinity)
        }
        .padding(.horizontal, 20).padding(.bottom, 12)
        .background(c.cream.ignoresSafeArea())
        .onAppear { best = model.carePlay.bestScore(id) }
        .sheet(isPresented: Binding(get: { result != nil }, set: { if !$0 && result != nil { model.pop() } })) {
            if let r = result {
                VStack(spacing: 4) {
                    Mascot(.happy, size: 96).padding(.top, 24)
                    if r.newBest { Overline("New personal best", c.toffee).padding(.top, 6) }
                    T("\(r.score)", .numeral)
                    T(r.line, .body, c.inkMuted, align: .center).padding(.top, 4).padding(.bottom, 20)
                    HStack(spacing: 10) {
                        TButton("Back to Play", kind: .quiet, fullWidth: true) { result = nil; model.pop() }
                        TButton("Play again", fullWidth: true) { result = nil; live = 0; best = model.carePlay.bestScore(id); round += 1 }
                    }
                }
                .padding(.horizontal, 20)
                .presentationDetents([.medium])
                .interactiveDismissDisabled(false)
                .sensoryFeedback(.success, trigger: r.newBest)
            }
        }
    }
}

typealias ScoreFn = (Int) -> Void
typealias EndFn = (Int, String) -> Void

// MARK: - Banana Catch

@Observable
final class BananaCatchModel {
    struct Faller: Identifiable { let id: Int; var x: Double; var y: Double; let rock: Bool; let speed: Double }
    var items: [Faller] = []
    var basketX = 0.5
    var lives = 3
    var score = 0
    var caught = 0
    var lostLife = 0
    @ObservationIgnored private var nextId = 0
    @ObservationIgnored private var spawn = 0.0
    @ObservationIgnored private var elapsed = 0.0

    /// One frame of the same physics as Android (difficulty rises over time; rocks 28% of the time).
    func step(_ dtIn: Double, onScore: ScoreFn) {
        let dt = min(dtIn, 0.05)
        elapsed += dt; spawn -= dt
        let difficulty = 1 + elapsed / 45
        if spawn <= 0 {
            items.append(Faller(id: nextId, x: Double.random(in: 0...0.84) + 0.08, y: -0.05, rock: Double.random(in: 0...1) < 0.28,
                                speed: (0.22 + Double.random(in: 0...0.1)) * difficulty))
            nextId += 1
            spawn = max(0.9 / difficulty, 0.35)
        }
        var keep: [Faller] = []
        for var f in items {
            f.y += f.speed * dt
            if f.y >= 0.88 && f.y <= 0.96 && abs(f.x - basketX) < 0.12 {
                if f.rock { lives -= 1; lostLife += 1 } else { score += 10; caught += 1; onScore(score) }
            } else if f.y <= 1.05 { keep.append(f) }
        }
        items = keep
    }
}

private struct BananaCatch: View {
    let onScore: ScoreFn
    let onEnd: EndFn
    @Environment(\.palette) private var c
    @State private var game = BananaCatchModel()
    @State private var ended = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 4) {
                ForEach(0..<3, id: \.self) { i in TIcon(Icons.drop, size: 18, color: i < game.lives ? c.period : c.tan) }
            }
            .accessibilityElement().accessibilityLabel("\(game.lives) lives left")
            GeometryReader { g in
                ZStack(alignment: .bottom) {
                    Canvas { ctx, sz in
                        for f in game.items {
                            let p = CGPoint(x: f.x * sz.width, y: f.y * sz.height)
                            if f.rock {
                                ctx.fill(Path(ellipseIn: CGRect(x: p.x - 17, y: p.y - 12, width: 34, height: 24)), with: .color(Color(hex: 0xB9ADA4)))
                                ctx.fill(Path(ellipseIn: CGRect(x: p.x - 9, y: p.y - 8, width: 10, height: 6)), with: .color(Color(hex: 0xD8CFC8)))
                            } else {
                                drawBanana(&ctx, at: p, scale: 1, outline: c.toffee.opacity(0.6), tip: c.toffee, fill: MascotColors.banana)
                            }
                        }
                        let bw: CGFloat = 96, bx = game.basketX * sz.width - bw / 2, by = sz.height * 0.92
                        var bowl = ctx
                        bowl.clip(to: Path(CGRect(x: 0, y: by, width: sz.width, height: sz.height)))
                        bowl.fill(Path(ellipseIn: CGRect(x: bx + 4, y: by - 26, width: bw - 8, height: 52)), with: .color(c.toffee))
                        ctx.fill(Path(roundedRect: CGRect(x: bx, y: by - 2, width: bw, height: 9), cornerRadius: 5), with: .color(c.periodDeep))
                    }
                    T("Drag anywhere to move", .caption, c.inkFaint).padding(.bottom, 4)
                }
                .background(c.oat, in: RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
                .contentShape(Rectangle())
                .gesture(DragGesture(minimumDistance: 0).onChanged { v in game.basketX = min(max(v.location.x / g.size.width, 0.08), 0.92) })
                .accessibilityElement()
                .accessibilityLabel("Game area. Bananas fall from the top; drag to move the basket.")
                .accessibilityAdjustableAction { dir in
                    game.basketX = min(max(game.basketX + (dir == .increment ? 0.1 : -0.1), 0.08), 0.92)
                }
            }
        }
        .sensoryFeedback(.impact(weight: .medium), trigger: game.lostLife)
        .task {
            var last = Date()
            while !Task.isCancelled && game.lives > 0 {
                try? await Task.sleep(nanoseconds: 16_000_000)
                let now = Date()
                game.step(now.timeIntervalSince(last), onScore: onScore)
                last = now
            }
            if game.lives <= 0 && !ended {
                ended = true
                onEnd(game.score, "You caught \(game.caught) \(game.caught == 1 ? "banana" : "bananas"). That's a nice two minutes off.")
            }
        }
    }
}

// MARK: - Mood Match

private struct MoodMatch: View {
    let onScore: ScoreFn
    let onEnd: EndFn
    @Environment(\.palette) private var c
    private static let symbols = [Icons.heat, Icons.moon, Icons.music, Icons.care, Icons.breath, Icons.book, Icons.food, Icons.spark]
    @State private var deck = Array(0..<8).flatMap { [$0, $0] }.shuffled()
    @State private var up: [Int] = []
    @State private var matched: Set<Int> = []
    @State private var moves = 0
    @State private var hint = "Find the pairs. Take your time."

    private func matchColor(_ i: Int) -> Color { [c.periodSoft, c.fertileSoft, c.bananaSoft, c.roseSoft, c.peachSoft, c.sand, c.fertileSoft, c.bananaSoft][i] }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            T("\(matched.count / 2) of \(deck.count / 2) pairs", .caption, c.inkMuted)
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 4), spacing: 10) {
                ForEach(deck.indices, id: \.self) { i in
                    let isUp = up.contains(i), isMatched = matched.contains(i)
                    Button { flip(i) } label: {
                        ZStack {
                            RoundedRectangle(cornerRadius: 12, style: .continuous).fill(isMatched ? matchColor(deck[i]) : isUp ? c.oat : c.peach)
                            if isUp { RoundedRectangle(cornerRadius: 12).stroke(c.ink, lineWidth: 2) }
                            if isUp || isMatched { TIcon(MoodMatch.symbols[deck[i]], size: 28) }
                            else { Circle().fill(c.peachSoft).frame(width: 20, height: 20) }
                        }
                        .aspectRatio(0.75, contentMode: .fit)
                        .rotation3DEffect(.degrees(isUp || isMatched ? 0 : 180), axis: (x: 0, y: 1, z: 0))
                        .animation(.easeOut(duration: 0.2), value: isUp || isMatched)
                    }
                    .buttonStyle(.plain)
                    .disabled(isUp || isMatched || up.count >= 2)
                    .accessibilityLabel(isUp || isMatched ? "Card \(deck[i] + 1)" : "Face-down card")
                }
            }
            Spacer()
            T(hint, .bodySm, c.inkMuted, align: .center).frame(maxWidth: .infinity).accessibilityAddTraits(.updatesFrequently)
        }
        .sensoryFeedback(.success, trigger: matched.count)
    }

    private func flip(_ i: Int) {
        up.append(i)
        guard up.count == 2 else { return }
        moves += 1; onScore(moves)
        let (a, b) = (up[0], up[1])
        if deck[a] == deck[b] {
            matched.formUnion([a, b]); up = []; hint = "A pair."
            if matched.count == deck.count { onEnd(max(120 - moves * 4, 20), "All pairs found in \(moves) moves.") }
        } else {
            hint = "Not a pair. They flip back in a moment."
            Task { try? await Task.sleep(nanoseconds: 800_000_000); up = [] }
        }
    }
}

// MARK: - Memory Flip (sequence memory)

private struct MemoryFlip: View {
    let onScore: ScoreFn
    let onEnd: EndFn
    @Environment(\.palette) private var c
    @State private var sequence = [Int.random(in: 0..<9)]
    @State private var showing = true
    @State private var lit = -1
    @State private var input = 0
    @State private var message = "Watch the order."
    @State private var over = false

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            T("Round \(sequence.count)", .caption, c.inkMuted)
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 3), spacing: 10) {
                ForEach(0..<9, id: \.self) { i in
                    Button { tap(i) } label: {
                        RoundedRectangle(cornerRadius: 16, style: .continuous).fill(lit == i ? c.banana : c.fertileSoft).aspectRatio(1, contentMode: .fit)
                    }
                    .buttonStyle(.plain)
                    .disabled(showing || over)
                    .accessibilityLabel("Tile \(i + 1)")
                }
            }
            Spacer()
            T(message, .bodySm, c.inkMuted, align: .center).frame(maxWidth: .infinity).accessibilityAddTraits(.updatesFrequently)
        }
        .task(id: sequence.count) { await play() }
        .sensoryFeedback(.selection, trigger: lit)
    }

    private func play() async {
        showing = true
        try? await Task.sleep(nanoseconds: 700_000_000)
        for t in sequence {
            lit = t; try? await Task.sleep(nanoseconds: 550_000_000)
            lit = -1; try? await Task.sleep(nanoseconds: 250_000_000)
        }
        showing = false; input = 0; message = "Your turn: repeat the order."
    }

    private func tap(_ i: Int) {
        lit = i
        Task { try? await Task.sleep(nanoseconds: 200_000_000); if !showing { lit = -1 } }
        if sequence[input] == i {
            input += 1
            if input == sequence.count {
                onScore(sequence.count * 10); message = "Yes. One more this time."
                sequence.append(Int.random(in: 0..<9))
            }
        } else {
            over = true
            onEnd((sequence.count - 1) * 10, "You remembered a sequence of \(sequence.count - 1).")
        }
    }
}

// MARK: - Brain Break (gentle sums)

struct SumQuestion: Equatable {
    let text: String
    let answer: Int
    let options: [Int]

    static func make(level: Int) -> SumQuestion {
        let maxV = 10 + level * 3
        let a = Int.random(in: 2..<maxV), b = Int.random(in: 1..<maxV)
        let plus = Bool.random() || a < b
        let ans = plus ? a + b : a - b
        var opts: Set<Int> = [ans]
        while opts.count < 4 {
            let o = ans + Int.random(in: -5...5)
            if o != ans && o >= 0 { opts.insert(o) }
        }
        return SumQuestion(text: plus ? "\(a) + \(b)" : "\(a) − \(b)", answer: ans, options: Array(opts).shuffled())
    }
}

private struct BrainBreak: View {
    let onScore: ScoreFn
    let onEnd: EndFn
    @Environment(\.palette) private var c
    @State private var level = 0
    @State private var q = SumQuestion.make(level: 0)
    @State private var score = 0
    @State private var right = 0
    @State private var asked = 1
    @State private var seconds = 60
    @State private var feedback = ""

    var body: some View {
        VStack(spacing: 10) {
            T("\(seconds) s", .caption, c.inkMuted).frame(maxWidth: .infinity, alignment: .leading)
            Spacer()
            T(q.text, .numeral).accessibilityLabel(q.text.replacingOccurrences(of: "−", with: "minus"))
            Spacer()
            T(feedback, .bodySm, c.inkMuted).padding(.bottom, 12).accessibilityAddTraits(.updatesFrequently)
            LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
                ForEach(q.options, id: \.self) { o in
                    Button { answer(o) } label: {
                        T("\(o)", .headline).frame(maxWidth: .infinity, minHeight: 64).background(c.sand, in: Capsule())
                    }
                    .buttonStyle(PressScale())
                }
            }
        }
        .sensoryFeedback(.success, trigger: right)
        .task {
            while seconds > 0 && !Task.isCancelled { try? await Task.sleep(nanoseconds: 1_000_000_000); if !Task.isCancelled { seconds -= 1 } }
            if !Task.isCancelled { onEnd(score, "\(right) right out of \(asked - 1). Your brain had a little workout.") }
        }
    }

    private func answer(_ o: Int) {
        guard seconds > 0 else { return }
        if o == q.answer { score += 10; right += 1; onScore(score); feedback = "Right."; if right % 4 == 0 { level += 1 } }
        else { feedback = "It was \(q.answer). Next one." }
        asked += 1; q = .make(level: level)
    }
}

// MARK: - Word Puzzle

private struct WordPuzzle: View {
    let onScore: ScoreFn
    let onEnd: EndFn
    @Environment(\.palette) private var c
    @State private var words = Array(PlayContent.words.shuffled().prefix(5))
    @State private var index = 0
    @State private var score = 0
    @State private var solved = 0
    @State private var letters: [Character] = []
    @State private var picked: [Int] = []
    @State private var hintUsed = false
    @State private var feedback = ""
    @State private var checking = false

    var body: some View {
        let word = Array(words[index].word)
        let guess = picked.map { letters[$0] }
        VStack(spacing: 0) {
            T("Word \(index + 1) of \(words.count)", .caption, c.inkMuted).frame(maxWidth: .infinity, alignment: .leading)
            T(words[index].hint, .title, align: .center).padding(.top, 24)
            HStack(spacing: 6) {
                ForEach(word.indices, id: \.self) { i in
                    let ch = i < guess.count ? String(guess[i]) : (hintUsed && i == 0 ? String(word[0]) : "")
                    T(ch, .title, i < guess.count ? c.ink : c.inkFaint).frame(width: 38, height: 48)
                        .background(c.oat, in: RoundedRectangle(cornerRadius: 10)).overlay(RoundedRectangle(cornerRadius: 10).stroke(c.inkFaint, lineWidth: 1.5))
                }
            }
            .padding(.vertical, 24)
            .accessibilityElement().accessibilityLabel("Your answer: \(String(guess).isEmpty ? "empty" : String(guess))")
            HStack(spacing: 8) {
                ForEach(letters.indices, id: \.self) { i in
                    let used = picked.contains(i)
                    Button { if !used && !checking { picked.append(i); check(word) } } label: {
                        T(used ? "" : String(letters[i]), .label).frame(width: 44, height: 44).background(used ? c.cream : c.sand, in: Circle())
                    }
                    .buttonStyle(PressScale())
                    .disabled(used)
                    .accessibilityLabel("Letter \(letters[i])")
                }
            }
            T(feedback, .bodySm, c.inkMuted).padding(.top, 16).accessibilityAddTraits(.updatesFrequently)
            Spacer()
            HStack(spacing: 10) {
                TButton("Undo", kind: .quiet, fullWidth: true) { if !picked.isEmpty { picked.removeLast() } }
                TButton("Hint", kind: .quiet, enabled: !hintUsed, fullWidth: true) { hintUsed = true }
                TButton("Skip", kind: .quiet, fullWidth: true) { next() }
            }
        }
        .onAppear { if letters.isEmpty { shuffleLetters() } }
        .onChange(of: index) { _, _ in shuffleLetters() }
    }

    private func shuffleLetters() {
        let w = Array(words[index].word)
        var s = w.shuffled()
        if s == w { s.reverse() }
        letters = s; picked = []; hintUsed = false; feedback = ""
    }

    private func next() {
        if index < words.count - 1 { index += 1 } else { onEnd(score, "You solved \(solved) of \(words.count) words.") }
    }

    private func check(_ word: [Character]) {
        let guess = picked.map { letters[$0] }
        guard guess.count == word.count else { return }
        checking = true
        if guess == word {
            score += hintUsed ? 15 : 20; solved += 1; onScore(score); feedback = "Yes, \(String(word))."
            Task { try? await Task.sleep(nanoseconds: 900_000_000); checking = false; next() }
        } else {
            feedback = "Not quite. Try another order."
            Task { try? await Task.sleep(nanoseconds: 700_000_000); picked = []; checking = false }
        }
    }
}

// MARK: - Calm Tap

private struct CalmTap: View {
    let onScore: ScoreFn
    let onEnd: EndFn
    @Environment(\.palette) private var c
    @State private var start = Date()
    @State private var score = 0
    @State private var taps = 0
    @State private var smooth = 0
    @State private var feedback = "Tap as the circle reaches the ring. There's no rush."
    @State private var tappedBreath = -1

    private static let breath = 4.4 // 4 s growth + 0.4 s pause, as on Android
    private func grow(at d: Date) -> Double {
        let t = d.timeIntervalSince(start).truncatingRemainder(dividingBy: CalmTap.breath)
        return t >= 4 ? 1.05 : 0.3 + (1.05 - 0.3) * t / 4
    }

    var body: some View {
        VStack(spacing: 0) {
            Spacer()
            TimelineView(.animation) { tl in
                let g = grow(at: tl.date)
                ZStack {
                    Circle().stroke(c.ink, lineWidth: 2).frame(width: 260, height: 260)
                    Circle().stroke(c.inkFaint, lineWidth: 1.5).frame(width: 232, height: 232)
                    Circle().fill(c.fertile).frame(width: 240, height: 240).scaleEffect(g)
                    T("Tap", .label, c.onBanana)
                }
            }
            .frame(width: 260, height: 260)
            .accessibilityHidden(true)
            T(feedback, .title, align: .center).padding(.top, 28).accessibilityAddTraits(.updatesFrequently)
            T("\(10 - taps) taps left · tap anywhere", .caption, c.inkFaint).padding(.top, 8)
            Spacer()
        }
        .frame(maxWidth: .infinity)
        .contentShape(Rectangle())
        .onTapGesture { tap() }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
        .accessibilityAction { tap() }
        .sensoryFeedback(.success, trigger: smooth)
    }

    private func tap() {
        let now = Date()
        let breathIndex = Int(now.timeIntervalSince(start) / CalmTap.breath)
        guard breathIndex != tappedBreath, taps < 10 else { return }
        tappedBreath = breathIndex; taps += 1
        let v = grow(at: now)
        if v >= 0.88 { score += 15; smooth += 1; onScore(score); feedback = "Smooth rhythm." }
        else { feedback = "A little early. Wait for the ring." }
        if taps >= 10 { onEnd(score, "\(smooth) of 10 taps in rhythm. Notice your breathing now.") }
    }
}

// MARK: - Focus Game

private struct FocusGame: View {
    let onScore: ScoreFn
    let onEnd: EndFn
    @Environment(\.palette) private var c
    private static let distractors = [Icons.moon, Icons.care, Icons.spark, Icons.drop, Icons.music, Icons.book]
    @State private var grid = Array(repeating: -1, count: 12)
    @State private var score = 0
    @State private var hits = 0
    @State private var misses = 0
    @State private var seconds = 45
    @State private var feedback = "Tap only the banana."

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            T("\(max(0, seconds)) s", .caption, c.inkMuted)
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 3), spacing: 10) {
                ForEach(0..<12, id: \.self) { i in
                    let v = grid[i]
                    Button { tap(i) } label: {
                        ZStack {
                            RoundedRectangle(cornerRadius: 16, style: .continuous).fill(c.sand)
                            if v == 99 { Mascot(.warm, size: 52) } else if v >= 0 { TIcon(FocusGame.distractors[v], size: 30, color: c.inkMuted) }
                        }
                        .aspectRatio(1.2, contentMode: .fit)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(v == 99 ? "Banana" : v >= 0 ? "Shape" : "Empty")
                }
            }
            Spacer()
            T(feedback, .bodySm, c.inkMuted, align: .center).frame(maxWidth: .infinity).accessibilityAddTraits(.updatesFrequently)
        }
        .sensoryFeedback(.success, trigger: hits)
        .sensoryFeedback(.warning, trigger: misses)
        .task {
            var elapsedMs = 0
            while seconds > 0 && !Task.isCancelled {
                let bananaAt = Double.random(in: 0...1) < 0.7 ? Int.random(in: 0..<12) : -1
                grid = (0..<12).map { i in i == bananaAt ? 99 : (Double.random(in: 0...1) < 0.6 ? Int.random(in: 0..<FocusGame.distractors.count) : -1) }
                let window = max(1300 - hits * 25, 650)
                try? await Task.sleep(nanoseconds: UInt64(window) * 1_000_000)
                elapsedMs += window
                seconds = 45 - elapsedMs / 1000
            }
            if !Task.isCancelled { onEnd(score, "\(hits) bananas spotted, \(misses) mis-taps. Nicely focused.") }
        }
    }

    private func tap(_ i: Int) {
        let v = grid[i]
        if v == 99 { score += 10; hits += 1; onScore(score); feedback = "Got it."; grid[i] = -1 }
        else if v >= 0 { misses += 1; feedback = "That one's not a banana." }
    }
}

// MARK: - Stress Smash

private struct StressSmash: View {
    let onScore: ScoreFn
    let onEnd: EndFn
    @Environment(\.palette) private var c
    @State private var crushed: Set<Int> = []
    @State private var total = 0
    @State private var hit = -1

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            T("Tap to crush. Keep going as long as it helps.", .bodySm, c.inkMuted).padding(.bottom, 6)
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 5), spacing: 8) {
                ForEach(0..<30, id: \.self) { i in
                    let isCrushed = crushed.contains(i)
                    Button {
                        crushed.insert(i); total += 1; hit = i; onScore(total)
                        Task { try? await Task.sleep(nanoseconds: 160_000_000); if hit == i { hit = -1 } }
                    } label: {
                        ZStack {
                            RoundedRectangle(cornerRadius: 12, style: .continuous).fill(hit == i ? c.banana : isCrushed ? c.cream : c.periodSoft)
                            if isCrushed { RoundedRectangle(cornerRadius: 12).stroke(c.hairline, lineWidth: 1.5); TIcon(Icons.check, size: 16, color: c.inkFaint) }
                        }
                        .aspectRatio(1, contentMode: .fit)
                        .scaleEffect(hit == i ? 0.9 : 1)
                        .animation(.spring(response: 0.2, dampingFraction: 0.5), value: hit)
                    }
                    .buttonStyle(.plain)
                    .disabled(isCrushed)
                    .accessibilityLabel(isCrushed ? "Crushed" : "Tile")
                }
            }
            Spacer()
            HStack(spacing: 10) {
                TButton("Fresh board", kind: .quiet, icon: Icons.refresh, fullWidth: true) { crushed = [] }
                TButton("I feel better", fullWidth: true) { onEnd(total, "\(total) tiles crushed. Glad that helped.") }
            }
        }
        .sensoryFeedback(.impact(weight: .light), trigger: total)
    }
}
