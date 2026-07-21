import SwiftUI
import KotlinModules

// Compose destinations, one UIViewControllerRepresentable each. All UI comes
// from the shared Kotlin module; Swift only supplies the system chrome —
// TabView, the search sheet, the detail action bar, and the long-press /
// ellipsis flight menus — which picks up Liquid Glass on iOS 26
// automatically. Detail navigation happens inside the Compose sheet (the map
// updates behind it), matching the reference demo.

// Invisible native button parked over the Compose avatar: tapping it presents
// the system account menu. The anchor streams the avatar's frame from Kotlin.
enum ProfileMenu {
    static func attach(to vc: UIViewController, anchor: ProfileMenuAnchor) {
        let button = UIButton(type: .custom)
        button.showsMenuAsPrimaryAction = true
        button.menu = UIMenu(children: [
            UIMenu(options: .displayInline, children: [
                UIAction(
                    title: GlassControllersKt.profileDisplayName(),
                    subtitle: "Edit Profile",
                    image: UIImage(systemName: "person.crop.circle")
                ) { _ in },
            ]),
            UIAction(title: "Manage Friends", image: UIImage(systemName: "person.2")) { _ in },
            UIAction(title: "Settings", image: UIImage(systemName: "gearshape")) { _ in },
        ])
        button.isHidden = true
        vc.view.addSubview(button)
        anchor.listener = { [weak vc] x, y, w, h in
            let frame = CGRect(
                x: CGFloat(truncating: x), y: CGFloat(truncating: y),
                width: CGFloat(truncating: w), height: CGFloat(truncating: h)
            )
            button.frame = frame
            button.isHidden = frame.isEmpty
            // Compose adds its canvas view after us; stay on top so taps
            // reach the button instead of the canvas.
            if let view = vc?.view, view.subviews.last !== button {
                view.bringSubviewToFront(button)
            }
        }
    }
}

struct FlightsTabView: UIViewControllerRepresentable {
    let onDetailShown: (String?) -> Void

    func makeCoordinator() -> FlightMenuCoordinator { FlightMenuCoordinator() }

    func makeUIViewController(context: Context) -> UIViewController {
        let anchor = ProfileMenuAnchor()
        let vc = GlassControllersKt.FlightsTabController(
            onDetailShown: onDetailShown,
            profileAnchor: anchor
        )
        // Real system context menus for the flight rows: Compose reports row
        // bounds, this interaction hit-tests them and previews the row.
        vc.view.addInteraction(UIContextMenuInteraction(delegate: context.coordinator))
        ProfileMenu.attach(to: vc, anchor: anchor)
        return vc
    }
    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}

// The flight menu, grouped like the reference demo. Defined once and rendered
// both as a UIMenu (long-press context menu) and as SwiftUI Menu content (the
// detail action bar's ellipsis button).

enum FlightMenu {
    enum Item: Hashable {
        case action(title: String, icon: String, destructive: Bool)
        case openInMaps(titles: [String])
    }

    /** Demo order, top to bottom. */
    static func groups(flightId: String) -> [[Item]] {
        let mapsTitles = (GlassControllersKt.flightMenuAirports(flightId: flightId) as? [String] ?? [])
            .map { "Open to \($0)" }
        return [
            [
                .action(title: "Get Pro", icon: "sparkles", destructive: false),
                .action(title: "Share", icon: "square.and.arrow.up", destructive: false),
            ],
            [
                .action(title: "Alternate Flights", icon: "arrow.triangle.branch", destructive: false),
                .action(title: "Contact Airline", icon: "phone", destructive: false),
                .openInMaps(titles: mapsTitles),
            ],
            [
                .action(title: "Enable Alerts", icon: "bell", destructive: false),
                .action(title: "Enable Live Activities", icon: "bolt", destructive: false),
            ],
            [
                .action(title: "Move To Friends", icon: "person.2", destructive: false),
                .action(title: "Report Data Issue", icon: "exclamationmark.bubble", destructive: false),
                .action(title: "Delete Flight", icon: "trash", destructive: true),
            ],
        ]
    }

    static func uiMenu(flightId: String) -> UIMenu {
        UIMenu(children: groups(flightId: flightId).map { group in
            UIMenu(options: .displayInline, children: group.map(uiElement))
        })
    }

    private static func uiElement(for item: Item) -> UIMenuElement {
        switch item {
        case let .action(title, icon, destructive):
            return UIAction(
                title: title,
                image: UIImage(systemName: icon),
                attributes: destructive ? .destructive : []
            ) { _ in }
        case let .openInMaps(titles):
            return UIMenu(title: "Open In Maps", image: UIImage(systemName: "map"),
                          children: titles.map { title in
                              UIAction(title: title, image: UIImage(systemName: "airplane")) { _ in }
                          })
        }
    }

    /**
     * When a menu presents upward from a bottom anchor, iOS places the first
     * declared item nearest the anchor — declare reversed so the visual order
     * still reads Get Pro → Delete, like the demo.
     */
    @ViewBuilder
    static func menuContent(flightId: String, bottomAnchored: Bool = false) -> some View {
        let ordered = bottomAnchored
            ? groups(flightId: flightId).reversed().map { Array($0.reversed()) }
            : groups(flightId: flightId)
        ForEach(Array(ordered.enumerated()), id: \.offset) { _, group in
            Section {
                ForEach(group, id: \.self) { item in
                    itemView(item)
                }
            }
        }
    }

    @ViewBuilder
    private static func itemView(_ item: Item) -> some View {
        switch item {
        case let .action(title, icon, destructive):
            Button(role: destructive ? .destructive : nil) { } label: {
                Label(title, systemImage: icon)
            }
        case let .openInMaps(titles):
            Menu {
                ForEach(titles, id: \.self) { title in
                    Button { } label: { Label(title, systemImage: "airplane") }
                }
            } label: {
                Label("Open In Maps", systemImage: "map")
            }
        }
    }
}

final class FlightMenuCoordinator: NSObject, UIContextMenuInteractionDelegate {
    func contextMenuInteraction(
        _ interaction: UIContextMenuInteraction,
        configurationForMenuAtLocation location: CGPoint
    ) -> UIContextMenuConfiguration? {
        guard let flightId = GlassControllersKt.flightMenuFlightIdAt(x: location.x, y: location.y) else {
            return nil
        }
        return UIContextMenuConfiguration(identifier: flightId as NSString, previewProvider: nil) { _ in
            FlightMenu.uiMenu(flightId: flightId)
        }
    }

    func contextMenuInteraction(
        _ interaction: UIContextMenuInteraction,
        previewForHighlightingMenuWithConfiguration configuration: UIContextMenuConfiguration
    ) -> UITargetedPreview? {
        guard
            let container = interaction.view,
            let flightId = configuration.identifier as? String,
            let r = GlassControllersKt.flightMenuRowRect(flightId: flightId)
        else { return nil }
        let rect = CGRect(
            x: CGFloat(r.get(index: 0)),
            y: CGFloat(r.get(index: 1)),
            width: CGFloat(r.get(index: 2)),
            height: CGFloat(r.get(index: 3))
        )
        guard let snapshot = container.resizableSnapshotView(
            from: rect, afterScreenUpdates: false, withCapInsets: .zero
        ) else { return nil }
        let parameters = UIPreviewParameters()
        parameters.visiblePath = UIBezierPath(
            roundedRect: CGRect(origin: .zero, size: rect.size), cornerRadius: 16
        )
        let target = UIPreviewTarget(
            container: container,
            center: CGPoint(x: rect.midX, y: rect.midY)
        )
        return UITargetedPreview(view: snapshot, parameters: parameters, target: target)
    }
}

// Native detail action bar: share / alerts / ellipsis cluster plus the Add
// Return pill, floating over the sheet bottom while a flight is open. The
// ellipsis presents the flight menu as a real system menu.
struct DetailActionBarView: View {
    let flightId: String

    var body: some View {
        HStack {
            HStack(spacing: 0) {
                barIcon("square.and.arrow.up")
                barIcon("bell.slash")
                Menu {
                    FlightMenu.menuContent(flightId: flightId, bottomAnchored: true)
                } label: {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(Color(red: 0.07, green: 0.07, blue: 0.09))
                        .frame(width: 37, height: 44)
                }
            }
            .padding(.horizontal, 10)
            .background(.white, in: Capsule())
            .shadow(color: .black.opacity(0.15), radius: 8, y: 2)

            Spacer()

            Button { } label: {
                Text("Add Return")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 22)
                    .frame(height: 48)
                    .background(Color(red: 0.18, green: 0.44, blue: 0.95), in: Capsule())
            }
            .shadow(color: .black.opacity(0.15), radius: 8, y: 2)
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 10)
    }

    private func barIcon(_ systemName: String) -> some View {
        Image(systemName: systemName)
            .font(.system(size: 17, weight: .semibold))
            .foregroundStyle(Color(red: 0.07, green: 0.07, blue: 0.09))
            .frame(width: 37, height: 44)
    }
}

struct FriendsTabView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let anchor = ProfileMenuAnchor()
        let vc = GlassControllersKt.FriendsTabController(profileAnchor: anchor)
        ProfileMenu.attach(to: vc, anchor: anchor)
        return vc
    }
    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}

struct PassportTabView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let anchor = ProfileMenuAnchor()
        let vc = GlassControllersKt.PassportTabController(profileAnchor: anchor)
        ProfileMenu.attach(to: vc, anchor: anchor)
        return vc
    }
    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}

struct AddFlightView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        GlassControllersKt.AddFlightController()
    }
    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}

struct FullComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        GlassControllersKt.FullComposeController()
    }
    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}

@available(iOS 26.0, *)
struct GlassContentView: View {
    @State private var selectedTab = 0
    @State private var showAddFlight = false
    @State private var detailFlightId: String? = nil

    var body: some View {
        TabView(selection: $selectedTab) {
            Tab("My Flights", systemImage: "airplane", value: 0) {
                FlightsTabView(onDetailShown: { id in
                    withAnimation { detailFlightId = id }
                })
                .ignoresSafeArea()
                // The demo replaces the tab bar with the detail action bar
                // while a flight is open.
                .toolbarVisibility(detailFlightId == nil ? .automatic : .hidden, for: .tabBar)
                .overlay(alignment: .bottom) {
                    if let flightId = detailFlightId {
                        DetailActionBarView(flightId: flightId)
                    }
                }
            }
            Tab("Friends", systemImage: "person.2.fill", value: 1) {
                FriendsTabView().ignoresSafeArea()
            }
            Tab("Passport", systemImage: "person.text.rectangle", value: 2) {
                PassportTabView().ignoresSafeArea()
            }
            Tab("Search", systemImage: "magnifyingglass", value: 3, role: .search) {
                // The search tab mirrors Flighty: it presents the Add Flight
                // sheet rather than owning content of its own.
                Color.clear
                    .onAppear { showAddFlight = true }
            }
        }
        .tabBarMinimizeBehavior(.automatic)
        .tint(.blue)
        .sheet(isPresented: $showAddFlight, onDismiss: { selectedTab = 0 }) {
            AddFlightView()
                .ignoresSafeArea()
                .presentationDetents([.large])
                .presentationDragIndicator(.visible)
        }
    }
}

struct RootView: View {
    var body: some View {
        if #available(iOS 26.0, *) {
            GlassContentView()
        } else {
            // Older iOS: the full-Compose app, unchanged.
            FullComposeView().ignoresSafeArea()
        }
    }
}

@main
struct GlassApp: App {
    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
