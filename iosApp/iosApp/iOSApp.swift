import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        DependenciesKt.doInitKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
