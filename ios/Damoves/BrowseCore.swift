import Foundation

// UI-independent browse behavior, also used by the headless behavior host.
protocol SoundPreferences {
    func load() -> Bool
    func save(_ enabled: Bool)
}

struct BrowseMove: Equatable {
    let id: String
    let name: String
    let imported: Bool

    init(id: String, name: String, imported: Bool = false) {
        self.id = id; self.name = name; self.imported = imported
    }
}

struct BrowseChoice: Equatable {
    let id: String
    let loop: Bool
}

struct BrowseCore {
    enum Action {
        case replaceMoves([BrowseMove])
        case search(String)
        case setSound(Bool)
        case restoreSound
        case open(String)
        case rollDice
        case clearChoice
        case requestDelete(String)
        case cancelDelete
        case confirmDelete
        case deleteFinished(Bool)
    }

    private(set) var moves: [BrowseMove] = []
    private(set) var query = ""
    private(set) var soundEnabled = true
    private(set) var choice: BrowseChoice?
    private(set) var pendingDelete: String?
    private(set) var deleting = false
    private(set) var deleteError: String?
    // The application uses random selection; tests inject a deterministic picker.
    private let chooseIndex: (Int) -> Int
    private let soundPreferences: SoundPreferences?

    init(chooseIndex: @escaping (Int) -> Int = { Int.random(in: 0..<$0) }, soundPreferences: SoundPreferences? = nil) {
        self.chooseIndex = chooseIndex
        self.soundPreferences = soundPreferences
        self.soundEnabled = soundPreferences?.load() ?? true
    }

    var visibleMoves: [BrowseMove] {
        moves.filter { MoveLogic.matches($0.name, query: query) }
    }

    mutating func send(_ action: Action) {
        switch action {
        case .replaceMoves(let moves):
            self.moves = moves
            choice = nil
        case .search(let query):
            self.query = query
            choice = nil
        case .setSound(let enabled):
            soundEnabled = enabled
            soundPreferences?.save(enabled)
        case .restoreSound:
            if let soundPreferences { soundEnabled = soundPreferences.load() }
        case .open(let id):
            choice = visibleMoves.contains { $0.id == id } ? BrowseChoice(id: id, loop: false) : nil
        case .rollDice:
            guard !moves.isEmpty else { choice = nil; return }
            let index = chooseIndex(moves.count)
            choice = moves.indices.contains(index) ? BrowseChoice(id: moves[index].id, loop: true) : nil
        case .clearChoice:
            choice = nil
        case .requestDelete(let id):
            guard !deleting else { return }
            pendingDelete = moves.contains { $0.id == id && $0.imported } ? id : nil
            deleteError = nil
        case .cancelDelete:
            if !deleting { pendingDelete = nil }
        case .confirmDelete:
            if pendingDelete != nil { deleting = true }
        case .deleteFinished(let success):
            guard deleting, let id = pendingDelete else { return }
            if success {
                moves.removeAll { $0.id == id }
                if choice?.id == id { choice = nil }
                deleteError = nil
            } else { deleteError = "Could not delete this clip." }
            pendingDelete = nil; deleting = false
        }
    }
}
