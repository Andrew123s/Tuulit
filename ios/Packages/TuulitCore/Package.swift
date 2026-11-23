// swift-tools-version:5.9
import PackageDescription

// Pure Swift domain and data layer shared by the Tuulit iOS app. No UIKit, SwiftUI or Firebase,
// so `swift test` runs it on any Swift toolchain (macOS, Linux CI, Swift for Windows).
let package = Package(
    name: "TuulitCore",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [.library(name: "TuulitCore", targets: ["TuulitCore"])],
    targets: [
        .target(name: "TuulitCore"),
        .testTarget(name: "TuulitCoreTests", dependencies: ["TuulitCore"]),
    ]
)
