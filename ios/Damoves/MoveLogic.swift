import Foundation

struct Move: Identifiable, Hashable {
    let url: URL
    let name: String
    let imported: Bool
    var id: URL { url }
}

enum MoveLogic {
    // AVFoundation support varies by codec; do not offer formats it cannot normally play.
    static let videoExtensions: Set<String> = ["mp4", "m4v", "mov", "3gp"]

    static func name(for url: URL) -> String {
        let words = url.deletingPathExtension().lastPathComponent.replacingOccurrences(of: "_", with: " ")
        guard let first = words.first else { return words }
        return first.uppercased() + words.dropFirst()
    }

    static func matches(_ name: String, query: String) -> Bool {
        let query = query.trimmingCharacters(in: .whitespacesAndNewlines)
        return query.isEmpty || name.localizedCaseInsensitiveContains(query)
    }
}

struct ClipRange {
    let duration: Double
    var start: Double = 0
    var end: Double

    init(duration: Double) {
        self.duration = max(0, duration)
        self.end = max(0, duration)
    }

    mutating func setStart(_ value: Double) {
        start = min(max(0, value), max(0, end - min(0.001, duration)))
    }

    mutating func setEnd(_ value: Double) {
        end = max(min(duration, value), min(duration, start + 0.001))
    }
}
