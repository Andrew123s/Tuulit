import Foundation
import TuulitCore

enum AITool: String { case chat, explain, myth, appointment }

struct AIReply { let text: String; let urgency: UrgencyLevel; let verdict: String? }

/// Sends questions to the Worker's /v1/ai endpoint (the same one Android uses). Conversations are
/// stored only on this device; the question and, if the person opted in, a minimal summary leave it.
final class AIService {
    private let api: APIClient
    private let conversations: ConversationRepository

    init(api: APIClient, conversations: ConversationRepository) { self.api = api; self.conversations = conversations }

    func ask(conversationId: String, tool: AITool, question: String, context: AiContext?) async throws -> AIReply {
        let local = MedicalSafetyGuard.assess(question)
        conversations.append(AiMessageRow(conversationId: conversationId, role: "user", text: question.trimmed, tool: tool.rawValue,
                                          urgency: local.level.rawValue, createdAt: Dates.nowMillis()))
        let history = conversations.conversation(conversationId).suffix(12).filter { !$0.failed }
        var body: [String: Any] = [
            "tool": tool.rawValue,
            "messages": history.map { ["role": $0.role, "content": $0.text] },
            "locale": Locale.current.identifier.replacingOccurrences(of: "_", with: "-"),
        ]
        if let context { body["context"] = context.json }
        do {
            let res = try await api.post("/v1/ai", body)
            let remote = UrgencyLevel(rawValue: res["urgency"] as? String ?? "NONE") ?? .NONE
            let worst = max(remote, local.level)
            let text = (res["text"] as? String ?? "").trimmed
            conversations.append(AiMessageRow(conversationId: conversationId, role: "assistant", text: text, tool: tool.rawValue,
                                              urgency: worst.rawValue, usedContext: context != nil, createdAt: Dates.nowMillis()))
            let verdict = (res["verdict"] as? String).flatMap { $0.isEmpty ? nil : $0 }
            return AIReply(text: text, urgency: worst, verdict: verdict)
        } catch {
            let message = (error as? APIError)?.errorDescription ?? "Something went wrong. Please try again."
            conversations.append(AiMessageRow(conversationId: conversationId, role: "assistant", text: message, tool: tool.rawValue,
                                              urgency: local.level.rawValue, failed: true, createdAt: Dates.nowMillis()))
            throw error
        }
    }
}

/// Voice-journal recordings in app-private storage (complete file protection, excluded from
/// backups). Signed-in people can keep a private R2 backup through a Worker-issued URL;
/// no R2 credential exists in the app. Files use Android's names so restores work across phones.
final class MediaService {
    private let api: APIClient

    init(api: APIClient) { self.api = api }

    private var dir: URL {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0].appendingPathComponent("voice", isDirectory: true)
        if !FileManager.default.fileExists(atPath: base.path) {
            try? FileManager.default.createDirectory(at: base, withIntermediateDirectories: true,
                                                     attributes: [.protectionKey: FileProtectionType.complete])
            var u = base
            var v = URLResourceValues(); v.isExcludedFromBackup = true
            try? u.setResourceValues(v)
        }
        return base
    }

    func localFile(_ day: Int) -> URL { dir.appendingPathComponent("voice_\(day).m4a") }
    func hasLocal(_ day: Int) -> Bool {
        let attrs = try? FileManager.default.attributesOfItem(atPath: localFile(day).path)
        return ((attrs?[.size] as? NSNumber)?.intValue ?? 0) > 0
    }

    /// Uploads the day's recording and returns its R2 object key.
    func backup(_ day: Int) async throws -> String {
        let file = localFile(day)
        let size = (try FileManager.default.attributesOfItem(atPath: file.path)[.size] as? NSNumber)?.intValue ?? 0
        let res = try await api.post("/v1/media/upload-url", ["kind": "voice", "contentType": "audio/mp4", "bytes": size])
        guard let s = res["url"] as? String, let url = URL(string: s), let key = res["key"] as? String else { throw APIError.server("Upload failed.") }
        try await api.putSigned(url, file: file, contentType: "audio/mp4")
        return key
    }

    func restore(_ day: Int, key: String) async throws {
        let res = try await api.post("/v1/media/download-url", ["key": key])
        guard let s = res["url"] as? String, let url = URL(string: s) else { throw APIError.server("Download failed.") }
        try await api.download(url, to: localFile(day))
    }

    func deleteRemote(_ key: String) async throws { _ = try await api.post("/v1/media/delete", ["key": key]) }
    func deleteLocal(_ day: Int) { try? FileManager.default.removeItem(at: localFile(day)) }
    func deleteAllLocal() { try? FileManager.default.removeItem(at: dir) }
}
