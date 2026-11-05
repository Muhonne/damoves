import SwiftUI
import AVFoundation

struct ImportView: View {
    let source: EditorSource
    @ObservedObject var store: MoveStore
    let sound: Bool
    @Environment(\.dismiss) private var dismiss
    @Environment(\.scenePhase) private var phase
    @State private var player = AVPlayer()
    @State private var range = ClipRange(duration: 0)
    @State private var name = ""
    @State private var ready = false
    @State private var previewing = false
    @State private var saving = false
    @State private var error: String?
    @State private var observer: Any?
    @State private var statusObserver: NSKeyValueObservation?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 12) {
                    VideoSurface(player: player).frame(height: 240).background(.black)
                    if ready {
                        Text(String(format: "Start %.2f s  —  End %.2f s", range.start, range.end))
                        RangeTimeline(range: $range, changed: rangeChanged).frame(height: 55)
                        HStack {
                            NudgeButton(title: "Start −") { nudge(start: true, by: -0.5) }
                            NudgeButton(title: "Start +") { nudge(start: true, by: 0.5) }
                            NudgeButton(title: "End −") { nudge(start: false, by: -0.5) }
                            NudgeButton(title: "End +") { nudge(start: false, by: 0.5) }
                        }.buttonStyle(.bordered)
                        HStack {
                            Button("−5s") { seekBy(-5) }
                            Spacer()
                            Button(previewing ? "Pause preview" : "Preview selection") {
                                if previewing { player.pause(); previewing = false }
                                else { seek(range.start); previewing = true; player.play() }
                            }
                            Spacer()
                            Button("+5s") { seekBy(5) }
                        }.buttonStyle(.bordered)
                        TextField("Move name", text: $name).textFieldStyle(.roundedBorder)
                        Text("Fast trim: the saved range may start at an earlier video keyframe.")
                            .font(.caption).foregroundStyle(.secondary)
                        Button(saving ? "Saving…" : "Save move") { save() }
                            .buttonStyle(.borderedProminent).disabled(saving)
                    } else { ProgressView() }
                }.padding()
            }
            .navigationTitle("Add clip").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() }.disabled(saving) } }
            .interactiveDismissDisabled(saving)
            .alert("Damoves", isPresented: Binding(get: { error != nil }, set: { if !$0 { error = nil } })) {
                Button("OK") { if !ready { dismiss() }; error = nil }
            } message: { Text(error ?? "") }
            .task { await prepare() }
            .onReceive(NotificationCenter.default.publisher(for: .AVPlayerItemDidPlayToEndTime)) { notification in
                if notification.object as? AVPlayerItem === player.currentItem && previewing {
                    player.pause(); previewing = false; seek(range.start)
                }
            }
            .onChange(of: phase) { if $0 == .background && !saving { dismiss() } }
            .onDisappear {
                player.pause()
                if let observer { player.removeTimeObserver(observer) }
                observer = nil
                statusObserver = nil
                player.replaceCurrentItem(with: nil)
                try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
                if source.temporary { try? FileManager.default.removeItem(at: source.url) }
            }
        }
    }

    private func prepare() async {
        name = source.name
        let asset = AVURLAsset(url: source.url)
        do {
            let duration = try await asset.load(.duration).seconds
            let playable = try await asset.load(.isPlayable)
            guard !Task.isCancelled else { return }
            guard playable, duration.isFinite, duration > 0 else { throw MoveStore.StoreError.export }
            range = ClipRange(duration: duration)
            let item = AVPlayerItem(asset: asset)
            statusObserver = item.observe(\.status, options: [.new]) { item, _ in
                if item.status == .failed { DispatchQueue.main.async { error = "Could not open this video."; ready = false } }
            }
            try? AVAudioSession.sharedInstance().setCategory(.playback)
            player.replaceCurrentItem(with: item)
            player.isMuted = !sound
            observer = player.addPeriodicTimeObserver(forInterval: CMTime(seconds: 0.05, preferredTimescale: 600), queue: .main) { time in
                if previewing && time.seconds >= range.end {
                    player.pause(); previewing = false; seek(range.start)
                }
            }
            ready = true
        } catch { self.error = "Could not open this video." }
    }

    private func seek(_ seconds: Double) {
        player.seek(to: CMTime(seconds: max(0, min(range.duration, seconds)), preferredTimescale: 600), toleranceBefore: .zero, toleranceAfter: .zero)
    }
    private func seekBy(_ seconds: Double) { seek(player.currentTime().seconds + seconds) }
    private func rangeChanged(_ start: Bool) {
        player.pause(); previewing = false
        seek(start ? range.start : max(0, range.end - 0.001))
    }
    private func nudge(start: Bool, by delta: Double) {
        if start { range.setStart(range.start + delta) } else { range.setEnd(range.end + delta) }
        rangeChanged(start)
    }
    private func save() {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { error = "Enter a move name."; return }
        player.pause(); previewing = false; saving = true
        Task {
            do { try await store.save(source: source.url, name: trimmed, range: range); saving = false; dismiss() }
            catch { saving = false; self.error = "Could not save this video. Try an MP4 video." }
        }
    }
}

private struct RangeTimeline: View {
    @Binding var range: ClipRange
    let changed: (Bool) -> Void
    @State private var draggingStart: Bool?
    var body: some View {
        GeometryReader { geometry in
            let width = max(1, geometry.size.width - 24)
            let startX = 12 + width * range.start / range.duration
            let endX = 12 + width * range.end / range.duration
            ZStack(alignment: .leading) {
                Capsule().fill(.gray).frame(height: 4).padding(.horizontal, 12)
                Capsule().fill(.blue).frame(width: max(0, endX - startX), height: 4).offset(x: startX)
                Circle().fill(.white).frame(width: 24, height: 24).offset(x: startX - 12)
                Circle().fill(.white).frame(width: 24, height: 24).offset(x: endX - 12)
            }.frame(maxHeight: .infinity)
                .contentShape(Rectangle())
                .gesture(DragGesture(minimumDistance: 0).onChanged { value in
                    if draggingStart == nil { draggingStart = abs(value.startLocation.x - startX) <= abs(value.startLocation.x - endX) }
                    let seconds = max(0, min(1, (value.location.x - 12) / width)) * range.duration
                    if draggingStart == true { range.setStart(seconds); changed(true) }
                    else { range.setEnd(seconds); changed(false) }
                }.onEnded { _ in draggingStart = nil })
        }.accessibilityLabel("Clip range. Use the buttons to adjust start and end.")
    }
}

private struct NudgeButton: View {
    let title: String
    let action: () -> Void
    @State private var repeatTask: Task<Void, Never>?
    var body: some View {
        Text(title).font(.caption).frame(maxWidth: .infinity).padding(.vertical, 10)
            .background(Color(white: 0.2), in: RoundedRectangle(cornerRadius: 8))
            .contentShape(Rectangle())
            .gesture(DragGesture(minimumDistance: 0).onChanged { _ in
                guard repeatTask == nil else { return }
                action()
                repeatTask = Task { @MainActor in
                    try? await Task.sleep(nanoseconds: 400_000_000)
                    while !Task.isCancelled {
                        action()
                        try? await Task.sleep(nanoseconds: 120_000_000)
                    }
                }
            }.onEnded { _ in repeatTask?.cancel(); repeatTask = nil })
            .accessibilityLabel("\(title), 0.5 seconds")
            .accessibilityAddTraits(.isButton).accessibilityAction { action() }
            .onDisappear { repeatTask?.cancel(); repeatTask = nil }
    }
}
