import SwiftUI
import UIKit
import UniformTypeIdentifiers
import TuulitCore

/// Explains why Ask can't run yet (no server URL, no Firebase, not signed in). Empty when ready.
struct AskGate: View {
    @Environment(AppModel.self) private var model
    var body: some View {
        if !model.api.isConfigured {
            Notice("Ask isn't connected yet", "This build has no Tuulit server address (TUULIT_API_BASE_URL). See docs/IOS_FIREBASE_SETUP.md.").padding(.top, 14)
        } else if model.auth.state == .unavailable {
            Notice("Ask needs an account", FirebaseService.notConfiguredMessage).padding(.top, 14)
        } else if model.auth.state == .signedOut {
            Notice(title: "Sign in to use Ask", message: "Questions go to Tuulit's server with your account so they can't be abused. Your conversations stay on this phone.") {
                TButton("Sign in or create an account", kind: .text) { model.push(.auth(.signIn)) }
            }
            .padding(.top, 14)
        }
    }
}

struct AskHomeView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var draft = ""

    private struct Shortcut { let label: String, sub: String, icon: IconDef, tool: String, topic: String? }
    private let shortcuts = [
        Shortcut(label: "Explain my cycle", sub: "Phases, simply", icon: Icons.book, tool: "explain", topic: "Explain the phase of my cycle I'm in right now"),
        Shortcut(label: "Why am I feeling this way?", sub: "Mood and energy", icon: Icons.care, tool: "chat", topic: "Why might I be feeling more emotional than usual?"),
        Shortcut(label: "Ask about a symptom", sub: "Cramps, bloating…", icon: Icons.pain, tool: "chat", topic: nil),
        Shortcut(label: "Prepare for my period", sub: "What helps ahead", icon: Icons.kit, tool: "chat", topic: "How can I prepare for my period this week?"),
        Shortcut(label: "Help me feel better", sub: "Gentle ideas", icon: Icons.heat, tool: "chat", topic: "I don't feel great today. What could help?"),
        Shortcut(label: "Doctor appointment", sub: "A summary to share", icon: Icons.edit, tool: "appointment", topic: nil),
    ]

    var body: some View {
        let tints = [c.fertileSoft, c.roseSoft, c.peachSoft, c.bananaSoft, c.periodSoft, c.sand]
        let iconTints = [c.lilacDeep, c.periodDeep, c.toffee, c.toffee, c.periodDeep, c.ink]
        ScreenScroll {
            ZStack(alignment: .topLeading) {
                c.fertileSoft
                Blob(seed: 5).fill(c.oat.opacity(0.6)).frame(width: 160, height: 160).frame(maxWidth: .infinity, alignment: .trailing).offset(x: 40, y: -30)
                VStack(alignment: .leading, spacing: 0) {
                    HStack(spacing: 12) {
                        Mascot(.warm, size: 46).frame(width: 56, height: 56).background(c.oat, in: Circle())
                        VStack(alignment: .leading, spacing: 2) { Overline("Ask Tuulit", c.lilacDeep); T("Hi, I'm here for you.", .title) }
                    }
                    T("What's on your mind?", .display).padding(.top, 14).accessibilityAddTraits(.isHeader)
                    T("Clear, kind answers about your body. Never a diagnosis.", .bodySm, c.inkMuted).padding(.top, 6)
                }
                .padding(22)
            }
            .clipShape(RoundedRectangle(cornerRadius: Radius.xl, style: .continuous))
            .padding(.top, 14)
            AskGate()
            T("Start with", .title).padding(.top, 22).padding(.bottom, 10)
            LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
                ForEach(shortcuts.indices, id: \.self) { i in
                    let sc = shortcuts[i]
                    Button { model.push(.askChat(tool: sc.tool, topic: sc.topic)) } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            TIcon(sc.icon, size: 20, color: iconTints[i]).frame(width: 38, height: 38).background(c.oat, in: Circle())
                            Spacer(minLength: 8)
                            T(sc.label, .label, lines: 2)
                            T(sc.sub, .caption, c.inkMuted, lines: 1)
                        }
                        .padding(14).frame(maxWidth: .infinity, minHeight: 124, alignment: .topLeading)
                        .background(tints[i], in: RoundedRectangle(cornerRadius: 24, style: .continuous))
                    }
                    .buttonStyle(PressScale())
                }
            }
            Composer(text: $draft, placeholder: "Ask about your body", enabled: true) {
                let q = draft.trimmed
                guard !q.isEmpty else { return }
                model.push(.askChat(tool: "chat", topic: q)); draft = ""
            }
            .padding(.top, 4)
            HStack {
                T(model.settings.aiContextOptIn ? "Personal answers on" : "Personal answers off · answers stay general", .caption, c.inkFaint)
                Spacer()
                TButton("Change", kind: .text) { model.push(.youPrivacy) }
            }
            .padding(.top, 8)
            TButton("Myth or fact? Check something you heard", kind: .text, icon: Icons.quiz) { model.push(.askChat(tool: "myth", topic: nil)) }
        }
    }
}

struct Composer: View {
    @Binding var text: String
    var placeholder: String
    var enabled: Bool
    let onSend: () -> Void
    @Environment(\.palette) private var c
    var body: some View {
        HStack {
            TextField(placeholder, text: $text, axis: .vertical)
                .lineLimit(1...4)
                .tStyle(.body)
                .submitLabel(.send)
                .onSubmit(onSend)
                .padding(.leading, 12)
                .onChange(of: text) { _, v in if v.count > 1000 { text = String(v.prefix(1000)) } }
            IconBtn(Icons.arrowUp, "Send", filled: true, enabled: enabled && !text.isBlank, action: onSend)
        }
        .padding(4)
        .background(c.oat, in: Capsule())
        .shadow(color: c.fertile.opacity(0.35), radius: 8, y: 2)
    }
}

/// Routes ask/chat/{tool}: appointment prep has its own screen.
struct AskChatRouter: View {
    let tool: String
    let topic: String?
    var body: some View {
        if tool == "appointment" { AppointmentPrepView() } else { AskChatView(tool: AITool(rawValue: tool) ?? .chat, topic: topic) }
    }
}

@MainActor
@Observable
final class AskModel {
    let tool: AITool
    let conversationId: String
    private(set) var messages: [AiMessageRow] = []
    var sending = false
    var error: String?
    var lastUrgency: UrgencyLevel = .NONE

    init(tool: AITool) { self.tool = tool; conversationId = "ask-\(tool.rawValue)" }

    func load(_ model: AppModel) { messages = model.conversations.conversation(conversationId) }

    func send(_ text: String, _ model: AppModel) {
        let q = text.trimmed
        guard !q.isEmpty, !sending else { return }
        sending = true; error = nil
        lastUrgency = MedicalSafetyGuard.assess(q).level
        let snap = model.snapshot
        let ctx = model.settings.aiContextOptIn && snap.prediction.hasRecordedCycles ? AiContext.from(snap.prediction, snap.insights) : nil
        Task {
            do {
                let r = try await model.ai.ask(conversationId: conversationId, tool: tool, question: q, context: ctx)
                if r.urgency > lastUrgency { lastUrgency = r.urgency }
            } catch {
                self.error = (error as? APIError)?.errorDescription ?? error.localizedDescription
            }
            sending = false
            load(model)
        }
        load(model)
    }

    func clear(_ model: AppModel) { model.conversations.clear(conversationId); lastUrgency = .NONE; error = nil; load(model) }
}

struct AskChatView: View {
    let tool: AITool
    let topic: String?
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var ask: AskModel?
    @State private var draft = ""
    @State private var sentTopic = false

    private static let explainTopics = ["follicular": "What happens in the follicular phase?", "ovulation": "What does ovulation feel like?"]

    var body: some View {
        let vm = ask ?? AskModel(tool: tool)
        let title = tool == .explain ? "Explain my body" : tool == .myth ? "Myth or fact?" : "Ask"
        VStack(spacing: 0) {
            AppBar(title: title, onBack: { model.pop() }) {
                if !vm.messages.isEmpty { IconBtn(Icons.trash, "Clear conversation") { vm.clear(model) } }
            }
            AskGate()
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 12) {
                        if vm.messages.isEmpty {
                            EmptyState(title: tool == .explain ? "What would you like explained?" : tool == .myth ? "What did you hear?" : "Ask anything about your cycle",
                                       message: tool == .myth ? "e.g. \"Every normal cycle is 28 days\"" : tool == .explain ? "e.g. \"Why do periods hurt?\"" : "Answers are general information, never a diagnosis.",
                                       mood: nil, icon: tool == .myth ? Icons.quiz : Icons.book)
                        }
                        ForEach(vm.messages) { m in
                            Bubble(message: m)
                            if m.role == "user", m.urgency == UrgencyLevel.URGENT.rawValue || m.urgency == UrgencyLevel.EMERGENCY.rawValue {
                                SafetyBlock(level: UrgencyLevel(rawValue: m.urgency) ?? .URGENT)
                            }
                        }
                        if vm.sending { Bubble(message: AiMessageRow(conversationId: "", role: "assistant", text: "…", createdAt: 0)) }
                        Color.clear.frame(height: 8).id("bottom")
                    }
                }
                .scrollDismissesKeyboard(.interactively)
                .onChange(of: vm.messages.count) { _, _ in withAnimation { proxy.scrollTo("bottom") } }
                .onChange(of: vm.sending) { _, _ in withAnimation { proxy.scrollTo("bottom") } }
            }
            if let e = vm.error { T(e, .caption, c.alert).padding(.bottom, 6) }
            if vm.messages.isEmpty && tool == .explain {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack { ForEach(["Why do periods hurt?", "What is the luteal phase?", "Why am I bloated?"], id: \.self) { s in Chip(s) { vm.send(s, model) } } }
                }
                .padding(.bottom, 10)
            }
            Composer(text: $draft, placeholder: tool == .myth ? "e.g. Every normal cycle is 28 days" : tool == .explain ? "What would you like explained?" : "Ask about your body",
                     enabled: !vm.sending) { vm.send(draft, model); draft = "" }
            T("General information · not a diagnosis", .caption, c.inkFaint).padding(.vertical, 8).frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 20)
        .background(c.cream.ignoresSafeArea())
        .onAppear {
            if ask == nil { let m = AskModel(tool: tool); m.load(model); ask = m }
            if !sentTopic, let t = topic, !t.isBlank, let a = ask {
                sentTopic = true
                a.send(AskChatView.explainTopics[t] ?? t, model)
            }
        }
    }
}

private struct Bubble: View {
    let message: AiMessageRow
    @Environment(\.palette) private var c
    var body: some View {
        let mine = message.role == "user"
        let typing = !mine && message.text == "…"
        HStack(alignment: .bottom, spacing: 8) {
            if mine { Spacer(minLength: 40) }
            if !mine { Mascot(.warm, size: 28).frame(width: 34, height: 34).background(c.bananaSoft, in: Circle()) }
            VStack(alignment: .leading, spacing: 0) {
                if typing { TypingDots() } else {
                    let paragraphs = message.text.components(separatedBy: "\n").filter { !$0.isBlank }
                    ForEach(Array(paragraphs.enumerated()), id: \.offset) { i, p in
                        let bullet = p.trimmed.hasPrefix("- ") || p.trimmed.hasPrefix("• ") || p.trimmed.range(of: #"^\d+[.)] "#, options: .regularExpression) != nil
                        T(p.trimmed, i == 0 && !mine && !message.failed ? .subtitle : .body)
                            .padding(.top, i == 0 ? 0 : bullet ? 4 : 8).padding(.leading, bullet ? 4 : 0)
                    }
                    if !mine && !message.failed {
                        T(message.usedContext ? "Used a summary of your cycle · not a diagnosis" : "General information · not a diagnosis", .caption, c.inkFaint).padding(.top, 8)
                    }
                }
            }
            .padding(.horizontal, 16).padding(.vertical, 12)
            .background(mine ? c.peach : message.failed ? c.alertSoft : c.oat,
                        in: UnevenRoundedRectangle(topLeadingRadius: 22, bottomLeadingRadius: mine ? 22 : 6, bottomTrailingRadius: mine ? 6 : 22, topTrailingRadius: 22))
            .frame(maxWidth: 290, alignment: mine ? .trailing : .leading)
            .accessibilityElement(children: .combine)
            .accessibilityLabel(typing ? "Tuulit is thinking" : (mine ? "You: " : "Tuulit: ") + message.text)
            if !mine { Spacer(minLength: 20) }
        }
        .frame(maxWidth: .infinity, alignment: mine ? .trailing : .leading)
    }
}

private struct TypingDots: View {
    @Environment(\.palette) private var c
    @Environment(\.tuulitReducedMotion) private var reduced
    var body: some View {
        TimelineView(.animation(minimumInterval: 0.05, paused: reduced)) { tl in
            HStack(spacing: 6) {
                ForEach(0..<3, id: \.self) { i in
                    let t = tl.date.timeIntervalSinceReferenceDate * 2 - Double(i) * 0.32
                    Circle().fill(c.fertile.opacity(reduced ? 1 : 0.3 + 0.7 * (0.5 + 0.5 * sin(t * .pi)))).frame(width: 9, height: 9)
                }
            }
            .padding(.vertical, 4)
        }
    }
}

private struct SafetyBlock: View {
    let level: UrgencyLevel
    @Environment(AppModel.self) private var model
    var body: some View {
        let a = MedicalSafetyGuard.assess(level == .EMERGENCY ? "chest pain" : "fever")
        VStack(spacing: 10) {
            Notice(a.headline, a.message, kind: .medical)
            HStack(spacing: 10) {
                if level == .EMERGENCY {
                    TButton("Call emergency", kind: .danger, icon: Icons.phone, fullWidth: true) {
                        // Opens the dialler without a number so the person dials their local emergency service.
                        if let url = URL(string: "tel://") { UIApplication.shared.open(url) }
                    }
                }
                TButton("Tell my person", kind: .quiet, icon: Icons.sos, fullWidth: true) { model.sheet = .sos }
            }
        }
    }
}

/// Appointment prep: a local summary from the person's own logs, optional AI-suggested
/// questions, and a PDF to share with a clinician.
struct AppointmentPrepView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var ask = AskModel(tool: .appointment)
    @State private var concern = ""
    @State private var loaded = false

    private var summary: [(String, String)] {
        let snap = model.snapshot, i = snap.insights
        var rows: [(String, String)] = []
        rows.append(("Cycle", i.averageCycleLength.map { avg in
            "\(avg) days avg\(i.cycleVariabilityDays.map { " (±\($0 / 2))" } ?? "") · period \(i.averagePeriodDuration ?? snap.settings.periodLength) days"
        } ?? "Not enough cycles logged yet"))
        if let p = i.painPatternAlert { rows.append(("Pain", "Severe (7+) in \(p.consecutiveHighPainCycles) cycles · affected daily life \(p.impactedActivitiesCount) times")) }
        if !i.symptomFrequency.isEmpty { rows.append(("Most logged", i.symptomFrequency.prefix(3).map(\.name).joined(separator: ", "))) }
        if let last = snap.cycles.last { rows.append(("Last period", Dates.long(last.startEpochDay))) }
        return rows
    }

    var body: some View {
        let questions = ask.messages.last { $0.role == "assistant" && !$0.failed }?.text
        let rows = summary
        ScreenScroll(bottomPad: 40) {
            AppBar("Appointment prep", onBack: { model.pop() })
            T("For your appointment", .display).padding(.top, 10)
            T("Built from your own logs on this phone.", .bodySm, c.inkMuted).padding(.top, 6).padding(.bottom, 10)
            ForEach(Array(rows.enumerated()), id: \.offset) { idx, r in ListRow(r.1, sub: r.0, last: idx == rows.count - 1) }
            GroupTitle("Questions to ask").padding(.top, 18)
            if let questions {
                Panel(padding: 16) { T(questions, .bodySm) }
                TButton("Start again", kind: .text) { ask.clear(model) }
            } else {
                Field(label: "Main concern (optional)", text: Binding(get: { concern }, set: { concern = $0.take(200) }), placeholder: "e.g. painful periods")
                T("Suggesting questions sends this summary (not your notes) to Tuulit's server.", .caption, c.inkFaint).padding(.top, 6)
                AskGate()
                TButton("Suggest questions", kind: .quiet, icon: Icons.spark, loading: ask.sending) {
                    ask.send("Concern: \(concern.isBlank ? "general check-up" : concern). Summary: " + rows.map { "\($0.0): \($0.1)" }.joined(separator: "; "), model)
                }
                .padding(.top, 10)
                if let e = ask.error { T(e, .caption, c.alert).padding(.top, 6) }
            }
            ShareLink(item: AppointmentPDF(rows: rows, questions: questions), preview: SharePreview("Appointment summary")) {
                HStack { TIcon(Icons.share, size: 18, color: c.onInk); T("Share as PDF", .label, c.onInk) }
                    .frame(maxWidth: .infinity, minHeight: 52).background(c.ink, in: Capsule())
            }
            .padding(.top, 18)
            T("A summary of your own records, not a diagnosis.", .caption, c.inkFaint).padding(.top, 8)
        }
        .onAppear { if !loaded { ask.load(model); loaded = true } }
    }
}

/// The appointment summary rendered to a one-page PDF on demand (same content as Android).
struct AppointmentPDF: Transferable {
    let rows: [(String, String)]
    let questions: String?

    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(exportedContentType: .pdf) { item in
            let url = FileManager.default.temporaryDirectory.appendingPathComponent("tuulit-appointment-\(Dates.iso(Dates.today())).pdf")
            let renderer = UIGraphicsPDFRenderer(bounds: CGRect(x: 0, y: 0, width: 595, height: 842))
            try renderer.writePDF(to: url) { ctx in
                ctx.beginPage()
                let title: [NSAttributedString.Key: Any] = [.font: UIFont.boldSystemFont(ofSize: 22)]
                let label: [NSAttributedString.Key: Any] = [.font: UIFont.systemFont(ofSize: 11), .foregroundColor: UIColor(red: 0.37, green: 0.33, blue: 0.29, alpha: 1)]
                let body: [NSAttributedString.Key: Any] = [.font: UIFont.systemFont(ofSize: 13)]
                var y: CGFloat = 48
                ("Tuulit · appointment summary" as NSString).draw(at: CGPoint(x: 48, y: y), withAttributes: title); y += 30
                ("Created \(Dates.long(Dates.today())) from the patient's own logs. Not a diagnosis." as NSString).draw(at: CGPoint(x: 48, y: y), withAttributes: label); y += 30
                for (k, v) in item.rows {
                    (k as NSString).draw(at: CGPoint(x: 48, y: y), withAttributes: label); y += 16
                    let r = (v as NSString).boundingRect(with: CGSize(width: 499, height: 200), options: .usesLineFragmentOrigin, attributes: body, context: nil)
                    (v as NSString).draw(in: CGRect(x: 48, y: y, width: 499, height: r.height), withAttributes: body); y += r.height + 14
                }
                if let q = item.questions {
                    y += 6
                    ("Questions to ask" as NSString).draw(at: CGPoint(x: 48, y: y), withAttributes: label); y += 18
                    (q as NSString).draw(in: CGRect(x: 48, y: y, width: 499, height: 800 - y), withAttributes: body)
                }
            }
            return SentTransferredFile(url)
        }
    }
}
