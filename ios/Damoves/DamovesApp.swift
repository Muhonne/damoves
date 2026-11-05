import SwiftUI
import AVFoundation
import AVKit
import PhotosUI
import UniformTypeIdentifiers
import CoreTransferable

@main
struct DamovesApp: App {
    var body: some Scene { WindowGroup { BrowseView().preferredColorScheme(.dark) } }
}

private struct Playback: Identifiable {
    let id = UUID()
    let move: Move
    let loop: Bool
}

struct EditorSource: Identifiable {
    let id = UUID()
    let url: URL
    let name: String
    let temporary: Bool
}

// Photos' temporary representation must be copied before its callback returns.
private struct PhotoVideo: Transferable {
    let url: URL
    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(importedContentType: .movie) { received in
            let dest = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
                .appendingPathExtension(received.file.pathExtension)
            try FileManager.default.copyItem(at: received.file, to: dest)
            return PhotoVideo(url: dest)
        }
    }
}

private struct SystemSoundPreferences: SoundPreferences {
    func load() -> Bool { UserDefaults.standard.object(forKey: "soundEnabled") as? Bool ?? true }
    func save(_ enabled: Bool) { UserDefaults.standard.set(enabled, forKey: "soundEnabled") }
}

struct BrowseView: View {
    @StateObject private var store = MoveStore()
    @Environment(\.scenePhase) private var phase
    @State private var query = ""
    @State private var browse = BrowseCore(soundPreferences: SystemSoundPreferences())
    @State private var navigation = NavigationCore()
    @State private var playback: Playback?
    @State private var editor: EditorSource?
    @State private var filePicker = false
    @State private var photo: PhotosPickerItem?
    @State private var deleting: Move?
    @State private var error: String?

    private var filtered: [Move] {
        let visible = Set(browse.visibleMoves.map(\.id))
        return store.moves.filter { visible.contains($0.url.absoluteString) }
    }

    private func updateMoves() {
        browse.send(.replaceMoves(store.moves.map { BrowseMove(id: $0.url.absoluteString, name: $0.name, imported: $0.imported) }))
    }

    private func playChoice() {
        guard navigation.screen == .browse, let choice = browse.choice,
              let move = store.moves.first(where: { $0.url.absoluteString == choice.id }) else { return }
        navigation.send(.openPlayer)
        playback = Playback(move: move, loop: choice.loop)
    }

    private func openEditor(_ source: EditorSource) {
        guard navigation.screen == .browse else {
            if source.temporary { try? FileManager.default.removeItem(at: source.url) }
            return
        }
        navigation.send(.openEditor)
        editor = source
    }

    var body: some View {
        VStack(spacing: 0) {
            VStack(spacing: 12) {
                HStack {
                    Text("Damoves").font(.headline)
                    Spacer()
                    Menu("Add clip") {
                        PhotosPicker(selection: $photo, matching: .videos) { Label("Photos", systemImage: "photo") }
                        Button { filePicker = true } label: { Label("Files", systemImage: "folder") }
                    }
                    Toggle(browse.soundEnabled ? "Sound on" : "Sound off", isOn: Binding(
                        get: { browse.soundEnabled },
                        set: { browse.send(.setSound($0)) }
                    )).font(.caption).fixedSize()
                }
                TextField("Search moves", text: $query)
                    .padding(12).background(Color(white: 0.12), in: RoundedRectangle(cornerRadius: 12))
                    .textInputAutocapitalization(.never).autocorrectionDisabled()
            }.padding([.horizontal, .bottom], 16)
            Divider()
            ZStack(alignment: .bottomTrailing) {
                ScrollView {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 3), spacing: 12) {
                        ForEach(filtered) { move in
                            Button { browse.send(.open(move.url.absoluteString)); playChoice() } label: { MoveCard(move: move) }
                                .buttonStyle(.plain)
                                .contextMenu {
                                    if move.imported {
                                        Button("Edit clip") { openEditor(EditorSource(url: move.url, name: move.name, temporary: false)) }
                                        Button("Delete", role: .destructive) {
                                            browse.send(.requestDelete(move.url.absoluteString))
                                            if browse.pendingDelete != nil { deleting = move }
                                        }
                                    }
                                }
                        }
                    }.padding(.horizontal, 16).padding(.top, 12).padding(.bottom, 88)
                }
                Button {
                    browse.send(.rollDice); playChoice()
                } label: {
                    Image(systemName: "die.face.5.fill").font(.title2).foregroundStyle(.black)
                        .frame(width: 56, height: 56).background(Color(red: 0.65, green: 0.71, blue: 0.84), in: Circle())
                }.accessibilityLabel("Play random move").padding(16)
            }
        }
        .background(Color(red: 0.06, green: 0.07, blue: 0.08))
        .onAppear { updateMoves(); browse.send(.restoreSound) }
        .onChange(of: store.moves) { _ in updateMoves() }
        .onChange(of: query) { browse.send(.search($0)) }
        .fullScreenCover(item: $playback, onDismiss: { navigation.send(.close); browse.send(.clearChoice) }) { selection in
            PlaybackView(selection: selection, sound: browse.soundEnabled) { playback = nil; error = "Could not play this video." }
        }
        .sheet(item: $editor, onDismiss: { navigation.send(.close); store.reload() }) { source in
            ImportView(source: source, store: store, sound: browse.soundEnabled)
        }
        .fileImporter(isPresented: $filePicker, allowedContentTypes: [.movie]) { result in
            do {
                let url = try result.get()
                let access = url.startAccessingSecurityScopedResource()
                defer { if access { url.stopAccessingSecurityScopedResource() } }
                let copy = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
                    .appendingPathExtension(url.pathExtension)
                try FileManager.default.copyItem(at: url, to: copy)
                openEditor(EditorSource(url: copy, name: "", temporary: true))
            } catch { self.error = "Could not open this video." }
        }
        .onChange(of: photo) { item in
            guard let item else { return }
            Task {
                do {
                    guard let video = try await item.loadTransferable(type: PhotoVideo.self) else { throw MoveStore.StoreError.export }
                    openEditor(EditorSource(url: video.url, name: "", temporary: true))
                } catch { self.error = "Could not open this video." }
                photo = nil
            }
        }
        .onChange(of: phase) { if $0 == .background { playback = nil } }
        .alert("Delete clip?", isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }), presenting: deleting) { move in
            Button("Delete", role: .destructive) {
                browse.send(.confirmDelete)
                guard browse.deleting else { return }
                do { try store.delete(move); browse.send(.deleteFinished(true)) }
                catch { browse.send(.deleteFinished(false)); self.error = browse.deleteError }
                deleting = nil
            }
            Button("Cancel", role: .cancel) { browse.send(.cancelDelete); deleting = nil }
        } message: { _ in Text("The original video stays where it is.") }
        .alert("Damoves", isPresented: Binding(get: { error != nil }, set: { if !$0 { error = nil } })) {
            Button("OK") { error = nil }
        } message: { Text(error ?? "") }
    }
}

private struct MoveCard: View {
    let move: Move
    @State private var image: UIImage?
    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .bottom) {
                Color(white: 0.15)
                if let image {
                    Image(uiImage: image).resizable().scaledToFill()
                        .frame(width: geometry.size.width, height: geometry.size.height).clipped()
                }
                Text(move.name).font(.subheadline).foregroundStyle(.white).frame(maxWidth: .infinity, alignment: .leading)
                    .padding(8).background(.black.opacity(0.82))
            }.clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .aspectRatio(9/12, contentMode: .fit)
        .accessibilityLabel(move.name)
        .task(id: move.id) {
            image = await Task.detached(priority: .utility) {
                let generator = AVAssetImageGenerator(asset: AVURLAsset(url: move.url))
                generator.appliesPreferredTrackTransform = true
                generator.maximumSize = CGSize(width: 360, height: 640)
                guard let cg = try? generator.copyCGImage(at: CMTime(seconds: 0.5, preferredTimescale: 600), actualTime: nil) else { return nil as UIImage? }
                return UIImage(cgImage: cg)
            }.value
        }
    }
}

struct VideoSurface: UIViewRepresentable {
    let player: AVPlayer
    final class Surface: UIView {
        override class var layerClass: AnyClass { AVPlayerLayer.self }
        var playerLayer: AVPlayerLayer { layer as! AVPlayerLayer }
    }
    func makeUIView(context: Context) -> Surface {
        let view = Surface()
        view.playerLayer.videoGravity = .resizeAspect
        view.playerLayer.player = player
        return view
    }
    func updateUIView(_ view: Surface, context: Context) { view.playerLayer.player = player }
}

private struct PlaybackView: View {
    let selection: Playback
    let sound: Bool
    let failed: () -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var player = AVPlayer()
    @State private var observation: NSKeyValueObservation?
    @State private var core: PlayerCore

    init(selection: Playback, sound: Bool, failed: @escaping () -> Void) {
        self.selection = selection
        self.sound = sound
        self.failed = failed
        _core = State(initialValue: PlayerCore(loop: selection.loop))
    }

    private func failPlayback() {
        guard !core.closed else { return }
        core.send(.failed)
        if core.error != nil { failed() }
    }

    var body: some View {
        VideoSurface(player: player).background(.black).ignoresSafeArea()
            .contentShape(Rectangle()).onTapGesture { core.send(.close); dismiss() }
            .accessibilityLabel("Close video").accessibilityAddTraits(.isButton)
            .onAppear {
                try? AVAudioSession.sharedInstance().setCategory(.playback)
                let item = AVPlayerItem(url: selection.move.url)
                observation = item.observe(\.status, options: [.initial, .new]) { item, _ in
                    if item.status == .failed { DispatchQueue.main.async { failPlayback() } }
                }
                player.replaceCurrentItem(with: item)
                player.isMuted = !sound
                player.play()
            }
            .onReceive(NotificationCenter.default.publisher(for: .AVPlayerItemDidPlayToEndTime)) { notification in
                guard notification.object as? AVPlayerItem === player.currentItem else { return }
                guard !core.closed else { return }
                core.send(.completed)
                if core.closed { dismiss() } else { player.seek(to: .zero); player.play() }
            }
            .onReceive(NotificationCenter.default.publisher(for: .AVPlayerItemFailedToPlayToEndTime)) { notification in
                if notification.object as? AVPlayerItem === player.currentItem { failPlayback() }
            }
            .onDisappear {
                core.send(.leave)
                player.pause(); player.replaceCurrentItem(with: nil); observation = nil
                try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
            }
    }
}
