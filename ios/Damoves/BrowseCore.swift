import Foundation

// UI-independent browse behavior, also used by the headless behavior host.
struct BrowseMove: Equatable {
    let id: String
    let name: String
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
        case open(String)
        case rollDice
        case clearChoice
    }

    private(set) var moves: [BrowseMove] = []
    private(set) var query = ""
    private(set) var soundEnabled = true
    private(set) var choice: BrowseChoice?
    // The application uses random selection; tests inject a deterministic picker.
    private let chooseIndex: (Int) -> Int

    init(chooseIndex: @escaping (Int) -> Int = { Int.random(in: 0..<$0) }) {
        self.chooseIndex = chooseIndex
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
        case .open(let id):
            choice = visibleMoves.contains { $0.id == id } ? BrowseChoice(id: id, loop: false) : nil
        case .rollDice:
            guard !moves.isEmpty else { choice = nil; return }
            let index = chooseIndex(moves.count)
            choice = moves.indices.contains(index) ? BrowseChoice(id: moves[index].id, loop: true) : nil
        case .clearChoice:
            choice = nil
        }
    }
}
