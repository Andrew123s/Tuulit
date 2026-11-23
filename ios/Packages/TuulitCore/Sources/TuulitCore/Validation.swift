import Foundation
#if canImport(CommonCrypto)
import CommonCrypto
#endif
#if canImport(Security)
import Security
#endif

/// Form validation shared with Android. Each returns an error message, or nil when valid.
public enum Validation {
    private static let emailRegex = try! NSRegularExpression(pattern: #"^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$"#)

    public static func email(_ value: String) -> String? {
        if value.isBlank { return "Enter your email address." }
        let v = value.trimmed
        if emailRegex.firstMatch(in: v, range: NSRange(v.startIndex..., in: v)) == nil { return "That doesn't look like an email address." }
        return nil
    }

    public static func password(_ value: String) -> String? {
        if value.count < 10 { return "Use at least 10 characters." }
        if !value.contains(where: { $0.isLetter }) || !value.contains(where: { $0.isNumber }) { return "Mix letters and at least one number." }
        return nil
    }

    public static func pin(_ value: String) -> String? {
        if !(4...6).contains(value.count) { return "Use 4 to 6 digits." }
        if !value.allSatisfy({ $0.isASCII && $0.isNumber }) { return "Digits only." }
        if Set(value).count == 1 { return "Avoid using the same digit throughout." }
        return nil
    }

    public static func displayName(_ value: String) -> String? { value.count > 40 ? "Keep it under 40 characters." : nil }
    public static func cycleLength(_ days: Int) -> String? { (15...60).contains(days) ? nil : "Cycle length must be between 15 and 60 days." }
    public static func periodLength(_ days: Int) -> String? { (1...14).contains(days) ? nil : "Period length must be between 1 and 14 days." }

    public static func inviteCode(_ value: String) -> String? {
        if value.count != 6 { return "Codes have 6 characters, like 4K8P2Q." }
        if !value.allSatisfy({ $0.isASCII && ($0.isLetter || $0.isNumber) }) { return "Letters and numbers only." }
        return nil
    }

    public static func shortText(_ value: String, max: Int = 120) -> String? {
        if value.isBlank { return "This can't be empty." }
        if value.count > max { return "Keep it under \(max) characters." }
        return nil
    }
}

/// PBKDF2-HMAC-SHA256 hashing for the app-lock PIN, in the same "pbkdf2:iter:salt:key" format
/// as Android. The PIN itself is never stored.
public enum PinHasher {
    public static var iterations = 120_000
    private static let keyBytes = 32

    public static func hash(_ pin: String, salt: [UInt8] = randomBytes(16)) -> String {
        let key = pbkdf2SHA256(password: Array(pin.utf8), salt: salt, iterations: iterations, keyLength: keyBytes)
        return "pbkdf2:\(iterations):\(Data(salt).base64EncodedString()):\(Data(key).base64EncodedString())"
    }

    public static func verify(_ pin: String, _ stored: String) -> Bool {
        let parts = stored.split(separator: ":", omittingEmptySubsequences: false).map(String.init)
        guard parts.count == 4, parts[0] == "pbkdf2", let iter = Int(parts[1]), iter > 0,
              let salt = Data(base64Encoded: parts[2]), let expected = Data(base64Encoded: parts[3]) else { return false }
        let key = pbkdf2SHA256(password: Array(pin.utf8), salt: Array(salt), iterations: iter, keyLength: expected.count)
        // Constant-time comparison.
        guard key.count == expected.count else { return false }
        var diff: UInt8 = 0
        for (a, b) in zip(key, expected) { diff |= a ^ b }
        return diff == 0
    }

    public static func randomBytes(_ n: Int) -> [UInt8] {
        var bytes = [UInt8](repeating: 0, count: n)
        #if canImport(Security)
        if SecRandomCopyBytes(kSecRandomDefault, n, &bytes) == errSecSuccess { return bytes }
        #endif
        var g = SystemRandomNumberGenerator()
        for i in 0..<n { bytes[i] = UInt8.random(in: 0...255, using: &g) }
        return bytes
    }

    public static func pbkdf2SHA256(password: [UInt8], salt: [UInt8], iterations: Int, keyLength: Int) -> [UInt8] {
        #if canImport(CommonCrypto)
        var out = [UInt8](repeating: 0, count: keyLength)
        let status = password.withUnsafeBufferPointer { pw in
            CCKeyDerivationPBKDF(CCPBKDFAlgorithm(kCCPBKDF2),
                                 UnsafeRawPointer(pw.baseAddress!).assumingMemoryBound(to: Int8.self), pw.count,
                                 salt, salt.count, CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA256), UInt32(iterations),
                                 &out, keyLength)
        }
        if status == kCCSuccess { return out }
        #endif
        return SoftCrypto.pbkdf2(password: password, salt: salt, iterations: iterations, keyLength: keyLength)
    }
}

/// Portable SHA-256 / HMAC / PBKDF2 used where CommonCrypto doesn't exist (Linux CI, Windows).
enum SoftCrypto {
    private static let k: [UInt32] = [
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
    ]

    static func sha256(_ message: [UInt8]) -> [UInt8] {
        var h: [UInt32] = [0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19]
        var msg = message
        let bitLen = UInt64(message.count) * 8
        msg.append(0x80)
        while msg.count % 64 != 56 { msg.append(0) }
        for i in (0..<8).reversed() { msg.append(UInt8((bitLen >> (UInt64(i) * 8)) & 0xff)) }
        var w = [UInt32](repeating: 0, count: 64)
        for chunk in stride(from: 0, to: msg.count, by: 64) {
            for i in 0..<16 {
                let j = chunk + i * 4
                w[i] = UInt32(msg[j]) << 24 | UInt32(msg[j + 1]) << 16 | UInt32(msg[j + 2]) << 8 | UInt32(msg[j + 3])
            }
            for i in 16..<64 {
                let s0 = rotr(w[i - 15], 7) ^ rotr(w[i - 15], 18) ^ (w[i - 15] >> 3)
                let s1 = rotr(w[i - 2], 17) ^ rotr(w[i - 2], 19) ^ (w[i - 2] >> 10)
                w[i] = w[i - 16] &+ s0 &+ w[i - 7] &+ s1
            }
            var a = h[0], b = h[1], c = h[2], d = h[3], e = h[4], f = h[5], g = h[6], hh = h[7]
            for i in 0..<64 {
                let s1 = rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25)
                let ch = (e & f) ^ (~e & g)
                let t1 = hh &+ s1 &+ ch &+ k[i] &+ w[i]
                let s0 = rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22)
                let maj = (a & b) ^ (a & c) ^ (b & c)
                let t2 = s0 &+ maj
                hh = g; g = f; f = e; e = d &+ t1; d = c; c = b; b = a; a = t1 &+ t2
            }
            h[0] = h[0] &+ a; h[1] = h[1] &+ b; h[2] = h[2] &+ c; h[3] = h[3] &+ d
            h[4] = h[4] &+ e; h[5] = h[5] &+ f; h[6] = h[6] &+ g; h[7] = h[7] &+ hh
        }
        return h.flatMap { v in [UInt8(v >> 24), UInt8((v >> 16) & 0xff), UInt8((v >> 8) & 0xff), UInt8(v & 0xff)] }
    }

    private static func rotr(_ x: UInt32, _ n: UInt32) -> UInt32 { (x >> n) | (x << (32 - n)) }

    static func hmac(key: [UInt8], message: [UInt8]) -> [UInt8] {
        var k = key.count > 64 ? sha256(key) : key
        k += [UInt8](repeating: 0, count: 64 - k.count)
        let inner = sha256(k.map { $0 ^ 0x36 } + message)
        return sha256(k.map { $0 ^ 0x5c } + inner)
    }

    static func pbkdf2(password: [UInt8], salt: [UInt8], iterations: Int, keyLength: Int) -> [UInt8] {
        var out: [UInt8] = []
        var block: UInt32 = 1
        while out.count < keyLength {
            var u = hmac(key: password, message: salt + [UInt8(block >> 24), UInt8((block >> 16) & 0xff), UInt8((block >> 8) & 0xff), UInt8(block & 0xff)])
            var t = u
            if iterations > 1 { for _ in 1..<iterations { u = hmac(key: password, message: u); for i in 0..<t.count { t[i] ^= u[i] } } }
            out += t
            block += 1
        }
        return Array(out.prefix(keyLength))
    }
}
