import Foundation
import CoreFoundation

private enum RequestError: Error { case invalid(String) }
private let actions = ["search", "setSound", "open", "rollDice", "clearChoice"]
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

private func describe(_ core: BrowseCore) -> [String: Any] {
    let choice: Any = core.choice.map { ["id": $0.id, "loop": $0.loop] as [String: Any] } ?? NSNull()
    return ["ok": true, "name": "Browse", "actions": actions,
            "state": ["query": core.query, "soundEnabled": core.soundEnabled,
                      "visibleMoves": core.visibleMoves.map { ["id": $0.id, "name": $0.name] },
                      "choice": choice]]
}

var core = BrowseCore()
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
            let moves = try list.map { BrowseMove(id: try string($0, "id"), name: try string($0, "name")) }
            guard Set(moves.map(\.id)).count == moves.count else { throw RequestError.invalid("Duplicate move id") }
            let index = indexValue.intValue
            let sound = try bool(request, "soundEnabled")
            core = BrowseCore(chooseIndex: { _ in index })
            core.send(.replaceMoves(moves))
            core.send(.setSound(sound))
        case "ls": break
        case "do":
            switch try string(request, "action") {
            case "search": core.send(.search(try string(request, "value")))
            case "setSound": core.send(.setSound(try bool(request, "enabled")))
            case "open": core.send(.open(try string(request, "id")))
            case "rollDice": core.send(.rollDice)
            case "clearChoice": core.send(.clearChoice)
            default: throw RequestError.invalid("Unknown action")
            }
        default: throw RequestError.invalid("Unknown operation")
        }
        response = describe(core)
    } catch {
        response = ["ok": false, "error": String(describing: error)]
    }
    let data = try! JSONSerialization.data(withJSONObject: response, options: [.sortedKeys])
    print(String(decoding: data, as: UTF8.self))
    fflush(stdout)
}
