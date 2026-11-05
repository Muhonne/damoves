import Foundation

// Screen decisions are independent of AVFoundation; milliseconds match Android.
struct ExportRequest {
    let name: String
    let start: Int
    let end: Int
}

struct EditorCore {
    enum Action {
        case prepare(duration: Int, start: Int, end: Int)
        case sourceFailed
        case setStart(Int)
        case setEnd(Int)
        case setName(String)
        case togglePreview
        case previewEnded
        case save
        case saveFinished(Bool)
        case clearError
    }

    private(set) var duration = 1
    private(set) var start = 0
    private(set) var end = 1
    private(set) var name = ""
    private(set) var previewing = false
    private(set) var saving = false
    private(set) var saved = false
    private(set) var error: String?
    private(set) var sourceFailed = false
    private(set) var prepared = false
    private(set) var pendingSave: ExportRequest?

    mutating func send(_ action: Action) {
        switch action {
        case let .prepare(duration, start, end):
            self.duration = max(1, duration)
            self.start = min(max(0, start), self.duration - 1)
            self.end = min(max(self.start + 1, end), self.duration)
            previewing = false; sourceFailed = false; prepared = true; error = nil
        case .sourceFailed:
            previewing = false; sourceFailed = true; prepared = false; error = "Could not open this video."
        case .setStart(let value):
            start = min(max(0, value), end - 1)
            previewing = false
        case .setEnd(let value):
            end = max(min(duration, value), start + 1)
            previewing = false
        case .setName(let value): name = value
        case .togglePreview: if !saving && prepared { previewing.toggle() }
        case .previewEnded: previewing = false
        case .save:
            guard !saving && prepared else { return }
            let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmed.isEmpty else {
                error = "Enter a move name"; return
            }
            pendingSave = ExportRequest(name: trimmed, start: start, end: end)
            error = nil; previewing = false; saving = true; saved = false
        case .saveFinished(let success):
            guard saving else { return }
            saving = false; saved = success; pendingSave = nil
            if !success { error = "Could not save this video. Try an MP4 video." }
        case .clearError: error = nil
        }
    }
}
