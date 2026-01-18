import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        InitKoinIosKt.doInitKoinIOS(
            buildEnvironment: EnvironmentConfig.getBuildEnvironment()
        )
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
