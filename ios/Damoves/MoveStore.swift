import Foundation
import AVFoundation

@MainActor
final class MoveStore: ObservableObject {
    @Published private(set) var moves: [Move] = []
    private struct Record: Codable {
        let file: String
        let name: String
    }
    private let directory: URL
    private var index: URL { directory.appendingPathComponent("moves.json") }

    init() {
        directory = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("Moves", isDirectory: true)
        reload()
    }

    private func records() throws -> [Record] {
        guard FileManager.default.fileExists(atPath: index.path) else { return [] }
        return try JSONDecoder().decode([Record].self, from: Data(contentsOf: index))
    }

    private func write(_ records: [Record]) throws {
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        try JSONEncoder().encode(records).write(to: index, options: .atomic)
    }

    func reload() {
        // A blue folder reference in the Xcode project preserves this directory in the bundle.
        let folder = Bundle.main.resourceURL?.appendingPathComponent("Videos", isDirectory: true)
        let bundled = (folder.flatMap { try? FileManager.default.contentsOfDirectory(at: $0, includingPropertiesForKeys: nil) } ?? [])
            .filter { MoveLogic.videoExtensions.contains($0.pathExtension.lowercased()) }
            .map { Move(url: $0, name: MoveLogic.name(for: $0), imported: false) }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
        let imported = ((try? records()) ?? []).compactMap { record -> Move? in
            // Never resolve an index entry outside our private directory.
            guard record.file == URL(fileURLWithPath: record.file).lastPathComponent else { return nil }
            let url = directory.appendingPathComponent(record.file)
            guard FileManager.default.fileExists(atPath: url.path) else { return nil }
            return Move(url: url, name: record.name, imported: true)
        }
        moves = bundled + imported
    }

    func save(source: URL, name: String, range: ClipRange) async throws {
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        // Read the index before exporting: do not replace a damaged index with an empty one.
        var existing = try records()
        let output = directory.appendingPathComponent(UUID().uuidString + ".mp4")
        let asset = AVURLAsset(url: source)
        guard let export = AVAssetExportSession(asset: asset, presetName: AVAssetExportPresetPassthrough),
              export.supportedFileTypes.contains(.mp4) else { throw StoreError.export }
        export.outputURL = output
        export.outputFileType = .mp4
        export.timeRange = CMTimeRange(start: CMTime(seconds: range.start, preferredTimescale: 600),
                                       end: CMTime(seconds: range.end, preferredTimescale: 600))
        do {
            try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                export.exportAsynchronously {
                    if export.status == .completed { continuation.resume() }
                    else { continuation.resume(throwing: export.error ?? StoreError.export) }
                }
            }
            existing.append(Record(file: output.lastPathComponent, name: name))
            try write(existing)
            reload()
        } catch {
            try? FileManager.default.removeItem(at: output)
            throw error
        }
    }

    func delete(_ move: Move) throws {
        guard move.imported, move.url.deletingLastPathComponent() == directory else { return }
        var existing = try records()
        existing.removeAll { $0.file == move.url.lastPathComponent }
        // Move the file aside until the index update succeeds, so a write failure
        // does not leave the index pointing at a deleted clip.
        let pending = directory.appendingPathComponent(UUID().uuidString + ".pending-delete")
        try FileManager.default.moveItem(at: move.url, to: pending)
        do { try write(existing) }
        catch {
            try? FileManager.default.moveItem(at: pending, to: move.url)
            throw error
        }
        try? FileManager.default.removeItem(at: pending)
        reload()
    }

    enum StoreError: Error { case export }
}
