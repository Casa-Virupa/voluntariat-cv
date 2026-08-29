import FirebaseCore
import SwiftUI
import Shared

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        return true
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    init() {
        InitKoinIosKt.doInitIOS(
            buildEnvironment: EnvironmentConfig.getBuildEnvironment()
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
