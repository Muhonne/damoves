import Foundation

// Playback decisions; the host owns the actual player and releases it.
struct PlayerCore {
    enum Action { case completed, close, leave, failed }
    let loop: Bool
    private(set) var closed = false
    private(set) var error: String?

    init(loop: Bool) { self.loop = loop }

    mutating func send(_ action: Action) {
        guard !closed else { return }
        switch action {
        case .completed: if !loop { closed = true }
        case .close, .leave: closed = true
        case .failed: closed = true; error = "Could not play this video."
        }
    }
}
