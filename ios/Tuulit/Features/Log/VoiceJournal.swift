import SwiftUI
import AVFoundation
import Speech
import TuulitCore

/// Voice journaling. "Dictate" turns speech into the day's note (on-device recognition when the
/// phone supports it). "Record" keeps an AAC audio note in app-private storage; signed-in people
/// can back it up to private R2 storage. The microphone is requested at the moment of use.
@MainActor
@Observable
final class VoiceRecorder: NSObject, AVAudioPlayerDelegate {
    enum Mode { case dictate, record, play }
    var mode: Mode? = nil
    var status: String? = nil
    private var recorder: AVAudioRecorder?
    private var player: AVAudioPlayer?
    private let engine = AVAudioEngine()
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var task: SFSpeechRecognitionTask?
    private var transcript = ""
    var onTranscript: ((String) -> Void)?

    private func micAllowed() async -> Bool {
        await withCheckedContinuation { cont in AVAudioApplication.requestRecordPermission { cont.resume(returning: $0) } }
    }

    func startRecording(to url: URL) async {
        guard await micAllowed() else { status = "Microphone access is off. You can allow it in Settings."; return }
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker])
            try session.setActive(true)
            let r = try AVAudioRecorder(url: url, settings: [
                AVFormatIDKey: Int(kAudioFormatMPEG4AAC), AVSampleRateKey: 44_100, AVNumberOfChannelsKey: 1, AVEncoderBitRateKey: 64_000,
            ])
            r.record(forDuration: 5 * 60)
            recorder = r; mode = .record; status = "Recording… up to 5 minutes."
        } catch {
            status = "Couldn't start recording."
        }
    }

    func play(_ url: URL) {
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback)
            let p = try AVAudioPlayer(contentsOf: url)
            p.delegate = self
            p.play(); player = p; mode = .play
        } catch { status = "Couldn't play the recording." }
    }

    nonisolated func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        Task { @MainActor in self.stop() }
    }

    func startDictation() async {
        guard await micAllowed() else { status = "Microphone access is off. You can allow it in Settings."; return }
        let auth = await withCheckedContinuation { cont in SFSpeechRecognizer.requestAuthorization { cont.resume(returning: $0) } }
        guard auth == .authorized, let recognizer = SFSpeechRecognizer(), recognizer.isAvailable else {
            status = "Speech recognition isn't available on this phone."; return
        }
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.record, mode: .measurement, options: .duckOthers)
            try session.setActive(true, options: .notifyOthersOnDeactivation)
            let req = SFSpeechAudioBufferRecognitionRequest()
            req.shouldReportPartialResults = true
            if recognizer.supportsOnDeviceRecognition { req.requiresOnDeviceRecognition = true }
            request = req
            transcript = ""
            let input = engine.inputNode
            input.removeTap(onBus: 0)
            input.installTap(onBus: 0, bufferSize: 1024, format: input.outputFormat(forBus: 0)) { [weak req] buf, _ in req?.append(buf) }
            engine.prepare()
            try engine.start()
            mode = .dictate; status = "Listening…"
            task = recognizer.recognitionTask(with: req) { [weak self] result, error in
                Task { @MainActor in
                    guard let self else { return }
                    if let result { self.transcript = result.bestTranscription.formattedString }
                    if error != nil || (result?.isFinal ?? false) { self.finishDictation() }
                }
            }
        } catch {
            status = "Dictation stopped."
        }
    }

    private func finishDictation() {
        guard mode == .dictate else { return }
        engine.stop(); engine.inputNode.removeTap(onBus: 0)
        request?.endAudio(); task?.cancel()
        request = nil; task = nil; mode = nil
        let text = transcript.trimmed
        if text.isEmpty { status = "Didn't catch that. Try again." } else { onTranscript?(text); status = "Added to your note." }
    }

    func stop() {
        switch mode {
        case .dictate: finishDictation(); return
        case .record:
            recorder?.stop(); recorder = nil; status = "Recording saved on this phone."
        case .play:
            player?.stop(); player = nil
        case nil: break
        }
        mode = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
}

struct VoiceJournal: View {
    let day: Int
    let hasKey: Bool
    let onTranscript: (String) -> Void
    let onBackedUp: (String) -> Void
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var voice = VoiceRecorder()
    @State private var hasRecording = false

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            T("Voice journal", .bodySm, c.inkMuted)
            HStack(spacing: 8) {
                if voice.mode == nil {
                    TButton("Dictate", kind: .quiet, icon: Icons.mic, small: true) { Task { await voice.startDictation() } }
                    TButton(hasRecording ? "Re-record" : "Record", kind: .quiet, icon: Icons.mic, small: true) {
                        Task { await voice.startRecording(to: model.media.localFile(day)) }
                    }
                    if hasRecording { TButton("Play", kind: .quiet, small: true) { voice.play(model.media.localFile(day)) } }
                } else {
                    TButton("Stop", small: true) { voice.stop(); hasRecording = model.media.hasLocal(day) }
                }
            }
            if hasRecording && voice.mode == nil {
                HStack(spacing: 8) {
                    if model.auth.state.isSignedIn && !hasKey {
                        TButton("Back up recording", kind: .text) {
                            voice.status = "Uploading…"
                            Task {
                                do { onBackedUp(try await model.media.backup(day)); voice.status = "Backed up privately." }
                                catch { voice.status = (error as? APIError)?.errorDescription ?? "Upload failed." }
                            }
                        }
                    }
                    TButton("Delete recording", kind: .text) {
                        model.media.deleteLocal(day); hasRecording = false; voice.status = "Recording deleted from this phone."
                    }
                }
            }
            if let s = voice.status { T(s, .caption, c.inkFaint).accessibilityAddTraits(.updatesFrequently) }
        }
        .padding(.top, 4)
        .onAppear { voice.onTranscript = onTranscript; hasRecording = model.media.hasLocal(day) }
        .onChange(of: day) { _, d in voice.stop(); hasRecording = model.media.hasLocal(d) }
        .onDisappear { voice.stop() }
    }
}
