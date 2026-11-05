import Foundation

// Controls which app screen may be opened; system pickers stay platform-owned.
struct NavigationCore {
    enum Screen: String { case browse, player, editor }
    enum Action { case openPlayer, openEditor, close }
    private(set) var screen: Screen = .browse

    mutating func send(_ action: Action) {
        switch action {
        case .openPlayer: if screen == .browse { screen = .player }
        case .openEditor: if screen == .browse { screen = .editor }
        case .close: screen = .browse
        }
    }
}
