import SwiftUI
import KotlinModules

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> some UIViewController {
        ViewControllerKt.ViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewControllerType, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView().ignoresSafeArea()
    }
}

@main
struct iosApp: App {
    init() {
        // Offered to the Kotlin side before the first screen is made, so alarms are available from the start.
        IosBridge.shared.alarms = AlarmKitBridge()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
