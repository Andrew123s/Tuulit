import Foundation

/// Errors from the Tuulit API, each with copy that can be shown as-is (same wording as Android).
enum APIError: LocalizedError, Equatable {
    case notConfigured, notSignedIn, offline, rateLimited
    case server(String)

    var errorDescription: String? {
        switch self {
        case .notConfigured: return "Tuulit's server isn't set up in this build yet (TUULIT_API_BASE_URL is missing)."
        case .notSignedIn: return "Sign in to use this."
        case .offline: return "You're offline. Your logs are safe on this phone."
        case .rateLimited: return "That's a lot of questions in a short time. Try again in a little while."
        case .server(let m): return m
        }
    }
}

/// Supplies the Firebase ID token and App Check token for each request.
protocol TokenSource: AnyObject {
    func idToken() async -> String?
    func appCheckToken() async -> String?
}

/// Thin client for the Tuulit Cloudflare Worker, the same one Android uses. The Worker holds every
/// secret (AI provider, Brevo, R2, Firebase service account); the app only sends a short-lived ID token.
final class APIClient {
    weak var tokens: TokenSource?
    let baseURL: String
    private let session: URLSession

    init(baseURL: String = Bundle.main.object(forInfoDictionaryKey: "TuulitAPIBaseURL") as? String ?? "", session: URLSession? = nil) {
        self.baseURL = baseURL.trimmingCharacters(in: .whitespaces)
        let cfg = URLSessionConfiguration.ephemeral
        cfg.timeoutIntervalForRequest = 60
        cfg.waitsForConnectivity = false
        cfg.urlCache = nil
        self.session = session ?? URLSession(configuration: cfg)
    }

    var isConfigured: Bool { baseURL.hasPrefix("https://") || baseURL.hasPrefix("http://127.0.0.1") || baseURL.hasPrefix("http://localhost") }

    func post(_ path: String, _ body: [String: Any] = [:], authenticated: Bool = true) async throws -> [String: Any] {
        guard isConfigured, let url = URL(string: baseURL.trimmingCharacters(in: CharacterSet(charactersIn: "/")) + path) else { throw APIError.notConfigured }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        req.httpBody = try JSONSerialization.data(withJSONObject: body)
        if authenticated {
            guard let token = await tokens?.idToken() else { throw APIError.notSignedIn }
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if let appCheck = await tokens?.appCheckToken() { req.setValue(appCheck, forHTTPHeaderField: "X-Firebase-AppCheck") }
        let data: Data, response: URLResponse
        do { (data, response) = try await session.data(for: req) } catch { throw APIError.offline }
        let code = (response as? HTTPURLResponse)?.statusCode ?? 0
        let json = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] ?? [:]
        switch code {
        case 200..<300: return json
        case 401, 403: throw APIError.notSignedIn
        case 429: throw APIError.rateLimited
        default:
            let msg = (json["error"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? "Something went wrong on our side (\(code)). Please try again."
            throw APIError.server(msg)
        }
    }

    /// Uploads a file to a pre-signed R2 URL issued by the Worker.
    func putSigned(_ url: URL, file: URL, contentType: String) async throws {
        var req = URLRequest(url: url)
        req.httpMethod = "PUT"
        req.setValue(contentType, forHTTPHeaderField: "Content-Type")
        let response: URLResponse
        do { (_, response) = try await session.upload(for: req, fromFile: file) } catch { throw APIError.offline }
        let code = (response as? HTTPURLResponse)?.statusCode ?? 0
        if !(200..<300).contains(code) { throw APIError.server("Upload failed (\(code)).") }
    }

    func download(_ url: URL, to dest: URL) async throws {
        let tmp: URL, response: URLResponse
        do { (tmp, response) = try await session.download(from: url) } catch { throw APIError.offline }
        let code = (response as? HTTPURLResponse)?.statusCode ?? 0
        if !(200..<300).contains(code) { throw APIError.server("Download failed (\(code)).") }
        try? FileManager.default.removeItem(at: dest)
        try FileManager.default.moveItem(at: tmp, to: dest)
    }
}
