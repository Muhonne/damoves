import Foundation
import CoreFoundation

private enum RequestError: Error { case invalid(String) }
private final class FixtureSound: SoundPreferences {
    var enabled: Bool
    init(_ enabled: Bool) { self.enabled = enabled }
    func load() -> Bool { enabled }
    func save(_ enabled: Bool) { self.enabled = enabled }
}
private let actions = ["search", "setSound", "open", "rollDice", "clearChoice", "requestDelete", "cancelDelete", "confirmDelete"]
private let editorActions = ["setStart", "setEnd", "setName", "togglePreview", "previewEnded", "save", "completeSave", "clearError"]
private let playerActions = ["completed", "close", "leave", "failed"]
private let navigationActions = ["openPlayer", "openEditor", "close"]
private func string(_ object: [String: Any], _ key: String) throws -> String {
    guard let value = object[key] as? String else { throw RequestError.invalid("\(key) must be a string") }
    return value
}

private func bool(_ object: [String: Any], _ key: String) throws -> Bool {
    guard let value = object[key] as? NSNumber, CFGetTypeID(value) == CFBooleanGetTypeID() else {
        throw RequestError.invalid("\(key) must be a boolean")
    }
    return value.boolValue
}

private func integer(_ object: [String: Any], _ key: String) throws -> Int {
    guard let value = object[key] as? NSNumber, CFGetTypeID(value) != CFBooleanGetTypeID(),
          value.doubleValue.rounded() == value.doubleValue,
          value.doubleValue >= Double(Int32.min), value.doubleValue <= Double(Int32.max) else {
        throw RequestError.invalid("\(key) must be an integer")
    }
    return value.intValue
}

private func describe(_ navigation: NavigationCore) -> [String: Any] {
    ["ok": true, "name": "Navigation", "actions": navigationActions,
     "state": ["screen": navigation.screen.rawValue]]
}

private func describe(_ player: PlayerCore) -> [String: Any] {
    ["ok": true, "name": "Player", "actions": playerActions,
     "state": ["loop": player.loop, "closed": player.closed, "error": player.error as Any? ?? NSNull()]]
}

private func describe(_ editor: EditorCore, lastExport: [String: Any]?, exportCount: Int) -> [String: Any] {
    ["ok": true, "name": "Editor", "actions": editorActions, "lastExport": lastExport as Any? ?? NSNull(), "exportCount": exportCount,
     "state": ["duration": editor.duration, "start": editor.start, "end": editor.end,
               "name": editor.name, "previewing": editor.previewing, "saving": editor.saving,
               "saved": editor.saved, "error": editor.error as Any? ?? NSNull(), "sourceFailed": editor.sourceFailed, "prepared": editor.prepared]]
}

private func describe(_ core: BrowseCore) -> [String: Any] {
    let choice: Any = core.choice.map { ["id": $0.id, "loop": $0.loop] as [String: Any] } ?? NSNull()
    return ["ok": true, "name": "Browse", "actions": actions,
            "state": ["query": core.query, "soundEnabled": core.soundEnabled,
                      "visibleMoves": core.visibleMoves.map { ["id": $0.id, "name": $0.name] },
                      "choice": choice, "pendingDelete": core.pendingDelete as Any? ?? NSNull(),
                      "deleting": core.deleting, "deleteError": core.deleteError as Any? ?? NSNull()]]
}

private var preferences = FixtureSound(true)
var diceIndex = 0
var core = BrowseCore(soundPreferences: preferences)
var editor = EditorCore()
var player = PlayerCore(loop: false)
var navigation = NavigationCore()
var focusedNavigation = false
var focusedEditor = false
var focusedPlayer = false
var saveSucceeds = true
var deferSave = false
var lastExport: [String: Any]?
var exportCount = 0
var deleteSucceeds = true
while let line = readLine() {
    let response: [String: Any]
    do {
        guard let request = try JSONSerialization.jsonObject(with: Data(line.utf8)) as? [String: Any] else {
            throw RequestError.invalid("Expected an object")
        }
        switch try string(request, "op") {
        case "init":
            guard let list = request["moves"] as? [[String: Any]],
                  let indexValue = request["randomIndex"] as? NSNumber,
                  CFGetTypeID(indexValue) != CFBooleanGetTypeID(),
                  indexValue.doubleValue.rounded() == indexValue.doubleValue,
                  indexValue.doubleValue >= Double(Int32.min), indexValue.doubleValue <= Double(Int32.max) else {
                throw RequestError.invalid("Invalid fixture")
            }
            let moves = try list.map { item in
                BrowseMove(id: try string(item, "id"), name: try string(item, "name"),
                           imported: item["imported"] == nil ? false : try bool(item, "imported"))
            }
            guard Set(moves.map(\.id)).count == moves.count else { throw RequestError.invalid("Duplicate move id") }
            let index = indexValue.intValue
            let sound = try bool(request, "soundEnabled")
            let outcome = request["deleteSucceeds"] == nil ? true : try bool(request, "deleteSucceeds")
            deleteSucceeds = outcome
            preferences = FixtureSound(sound)
            diceIndex = index
            core = BrowseCore(chooseIndex: { _ in index }, soundPreferences: preferences)
            core.send(.replaceMoves(moves))
            focusedEditor = false; focusedPlayer = false; focusedNavigation = false
        case "restart":
            guard !focusedEditor && !focusedPlayer && !focusedNavigation else { throw RequestError.invalid("Restart requires Browse") }
            let moves = core.moves
            let index = diceIndex
            core = BrowseCore(chooseIndex: { _ in index }, soundPreferences: preferences)
            core.send(.replaceMoves(moves))
        case "initNavigation":
            navigation = NavigationCore()
            focusedEditor = false; focusedPlayer = false; focusedNavigation = true
        case "initPlayer":
            let loop = try bool(request, "loop")
            player = PlayerCore(loop: loop)
            focusedEditor = false; focusedPlayer = true; focusedNavigation = false
        case "initEditor":
            let duration = try integer(request, "duration")
            let name = try string(request, "name")
            let outcome = try bool(request, "saveSucceeds")
            let available = request["sourceAvailable"] == nil ? true : try bool(request, "sourceAvailable")
            let deferred = request["deferSave"] == nil ? false : try bool(request, "deferSave")
            guard duration > 0 else { throw RequestError.invalid("duration must be positive") }
            editor = EditorCore()
            if available { editor.send(.prepare(duration: duration, start: 0, end: duration)) }
            else { editor.send(.sourceFailed) }
            editor.send(.setName(name))
            saveSucceeds = outcome
            deferSave = deferred
            lastExport = nil; exportCount = 0
            focusedEditor = true; focusedPlayer = false; focusedNavigation = false
        case "ls": break
        case "do" where focusedNavigation:
            switch try string(request, "action") {
            case "openPlayer": navigation.send(.openPlayer)
            case "openEditor": navigation.send(.openEditor)
            case "close": navigation.send(.close)
            default: throw RequestError.invalid("Unknown action")
            }
        case "do" where focusedPlayer:
            switch try string(request, "action") {
            case "completed": player.send(.completed)
            case "close": player.send(.close)
            case "leave": player.send(.leave)
            case "failed": player.send(.failed)
            default: throw RequestError.invalid("Unknown action")
            }
        case "do" where focusedEditor:
            switch try string(request, "action") {
            case "setStart": editor.send(.setStart(try integer(request, "value")))
            case "setEnd": editor.send(.setEnd(try integer(request, "value")))
            case "setName": editor.send(.setName(try string(request, "value")))
            case "togglePreview": editor.send(.togglePreview)
            case "previewEnded": editor.send(.previewEnded)
            case "save":
                let wasSaving = editor.saving
                editor.send(.save)
                if !wasSaving, let request = editor.pendingSave {
                    lastExport = ["name": request.name, "start": request.start, "end": request.end]; exportCount += 1
                    if !deferSave { editor.send(.saveFinished(saveSucceeds)) }
                }
            case "completeSave": editor.send(.saveFinished(saveSucceeds))
            case "clearError": editor.send(.clearError)
            default: throw RequestError.invalid("Unknown action")
            }
        case "do":
            switch try string(request, "action") {
            case "search": core.send(.search(try string(request, "value")))
            case "setSound": core.send(.setSound(try bool(request, "enabled")))
            case "open": core.send(.open(try string(request, "id")))
            case "rollDice": core.send(.rollDice)
            case "clearChoice": core.send(.clearChoice)
            case "requestDelete": core.send(.requestDelete(try string(request, "id")))
            case "cancelDelete": core.send(.cancelDelete)
            case "confirmDelete":
                core.send(.confirmDelete)
                if core.deleting { core.send(.deleteFinished(deleteSucceeds)) }
            default: throw RequestError.invalid("Unknown action")
            }
        default: throw RequestError.invalid("Unknown operation")
        }
        response = focusedNavigation ? describe(navigation) : focusedPlayer ? describe(player) : focusedEditor ? describe(editor, lastExport: lastExport, exportCount: exportCount) : describe(core)
    } catch {
        response = ["ok": false, "error": String(describing: error)]
    }
    let data = try! JSONSerialization.data(withJSONObject: response, options: [.sortedKeys])
    print(String(decoding: data, as: UTF8.self))
    fflush(stdout)
}
