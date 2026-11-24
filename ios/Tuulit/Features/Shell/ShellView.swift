import SwiftUI
import UIKit
import TuulitCore

/// Tabs with their own navigation stacks, the floating pill tab bar with the banana Log button,
/// shell sheets and toasts. Android's equivalent is `Shell.kt` + `NavGraph.kt`.
struct ShellView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c

    private var showTabBar: Bool {
        let atRoot = model.paths[model.tab]?.isEmpty ?? true
        let quiet = model.tab == .today && model.settings.notTheDayOn == model.snapshot.today
        return atRoot && !quiet
    }

    var body: some View {
        @Bindable var m = model
        ZStack(alignment: .bottom) {
            ZStack {
                tabStack(.today) { TodayView() }.opacity(model.tab == .today ? 1 : 0).allowsHitTesting(model.tab == .today)
                tabStack(.cycle) { CycleView() }.opacity(model.tab == .cycle ? 1 : 0).allowsHitTesting(model.tab == .cycle)
                tabStack(.care) { CareView() }.opacity(model.tab == .care ? 1 : 0).allowsHitTesting(model.tab == .care)
                tabStack(.ask) { AskHomeView() }.opacity(model.tab == .ask ? 1 : 0).allowsHitTesting(model.tab == .ask)
            }
            if showTabBar { TabBar().transition(.move(edge: .bottom).combined(with: .opacity)) }
            if let t = model.toast { ToastView(toast: t).padding(.bottom, showTabBar ? 110 : 30).transition(.opacity) }
        }
        .animation(.easeOut(duration: 0.2), value: showTabBar)
        .animation(.easeOut(duration: 0.2), value: model.toast)
        .sheet(item: $m.logDay) { d in LogComposerView(startDay: d.day).presentationDragIndicator(.visible) }
        .sheet(item: $m.sheet) { s in
            Group {
                switch s {
                case .needSomething: NeedSomethingSheet()
                case .sos: SOSSheet()
                case .bananaMode: BananaModeSheet()
                }
            }
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
        }
    }

    private func tabStack<Root: View>(_ tab: Tab, @ViewBuilder root: () -> Root) -> some View {
        @Bindable var m = model
        return NavigationStack(path: Binding(get: { m.paths[tab] ?? [] }, set: { m.paths[tab] = $0 })) {
            root()
                .toolbar(.hidden, for: .navigationBar)
                .navigationDestination(for: Route.self) { r in
                    RouteView(route: r).toolbar(.hidden, for: .navigationBar)
                }
        }
    }
}

/// Maps a route to its screen.
struct RouteView: View {
    let route: Route
    var body: some View {
        switch route {
        case .insights: InsightsView()
        case .pain: PainJournalView()
        case .history: HistoryView()
        case .session(let id): SessionView(id: id)
        case .game(let id): GameView(id: id)
        case .quiz(let id): QuizView(id: id)
        case .askChat(let tool, let topic): AskChatRouter(tool: tool, topic: topic)
        case .you: YouView()
        case .youCycle: CycleSettingsView()
        case .youBanana: BananaModeView()
        case .youPrivacy: PrivacyView()
        case .youNotifications: NotificationsView()
        case .youAppearance: AppearanceView()
        case .youAccount: AccountView()
        case .youHelp: HelpView()
        case .auth(let mode): AuthView(startMode: mode)
        case .connect: ConnectView()
        case .connectInvite: InviteView()
        case .connectAccept: AcceptInviteView()
        case .connectPerson(let id): PersonView(id: id)
        case .partner(let id): PartnerView(id: id)
        }
    }
}

private struct TabBar: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @Environment(\.tuulitReducedMotion) private var reduced
    @State private var pressed = false

    var body: some View {
        HStack(spacing: 0) {
            item(.today, "Today", Icons.today)
            item(.cycle, "Cycle", Icons.cycle)
            Color.clear.frame(width: 78)
            item(.care, "Care", Icons.care)
            item(.ask, "Ask", Icons.ask)
        }
        .padding(.horizontal, 8)
        .frame(height: 76)
        .background(c.oat, in: RoundedRectangle(cornerRadius: 34, style: .continuous))
        .shadow(color: c.period.opacity(0.18), radius: 18, y: 6)
        .overlay(alignment: .top) {
            Button { model.openLog(model.snapshot.today) } label: {
                ZStack {
                    Circle().fill(c.oat).frame(width: 76, height: 76)
                    Circle().fill(c.banana).frame(width: 66, height: 66)
                    TIcon(Icons.plus, size: 30, color: c.onBanana).rotationEffect(.degrees(pressed && !reduced ? 90 : 0))
                }
            }
            .buttonStyle(LogButtonStyle(pressed: $pressed))
            .offset(y: -26)
            .accessibilityLabel("Log")
            .accessibilityHint("Log today's period, mood and symptoms")
            .accessibilityIdentifier("tab.log")
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 4)
        .sensoryFeedback(.impact(weight: .light), trigger: model.logDay)
    }

    private func item(_ tab: Tab, _ label: String, _ icon: IconDef) -> some View {
        let on = model.tab == tab
        return Button {
            if model.tab == tab { model.paths[tab] = [] } else { model.tab = tab }
        } label: {
            VStack(spacing: 4) {
                TIcon(icon, size: 24, color: on ? c.periodDeep : c.inkMuted)
                T(label, .tab, on ? c.ink : c.inkMuted, lines: 1)
            }
            .frame(maxWidth: .infinity, minHeight: 60)
            .background(on ? c.peachSoft : .clear, in: RoundedRectangle(cornerRadius: 26, style: .continuous))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .accessibilityAddTraits(on ? [.isSelected, .isButton] : .isButton)
        .accessibilityIdentifier("tab.\(label.lowercased())")
    }
}

private struct LogButtonStyle: ButtonStyle {
    @Binding var pressed: Bool
    @Environment(\.tuulitReducedMotion) private var reduced
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed && !reduced ? 0.9 : 1)
            .animation(reduced ? nil : .spring(response: 0.25, dampingFraction: 0.6), value: configuration.isPressed)
            .onChange(of: configuration.isPressed) { _, p in pressed = p }
    }
}

private struct ToastView: View {
    let toast: Toast
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c

    var body: some View {
        HStack(spacing: 10) {
            TIcon(Icons.check, size: 18, color: c.onInk)
            T(toast.text, .label, c.onInk)
            if let label = toast.actionLabel {
                Button(label) { model.runToastAction() }
                    .tStyle(.label, c.onInk).underline()
            }
        }
        .padding(.horizontal, 18).padding(.vertical, 12)
        .background(c.ink, in: Capsule())
        .accessibilityElement(children: .combine)
        .onAppear { UIAccessibility.post(notification: .announcement, argument: toast.text) }
    }
}

/// Keeps the edge-swipe back gesture working while the system navigation bar is hidden.
extension UINavigationController: @retroactive UIGestureRecognizerDelegate {
    override open func viewDidLoad() {
        super.viewDidLoad()
        interactivePopGestureRecognizer?.delegate = self
    }

    public func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        viewControllers.count > 1
    }
}
