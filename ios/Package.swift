// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "DamovesLogic",
    products: [.library(name: "DamovesLogic", targets: ["DamovesLogic"])],
    targets: [
        .target(name: "DamovesLogic", path: "Damoves", exclude: ["DamovesApp.swift", "ImportView.swift", "MoveStore.swift", "Videos"], sources: ["MoveLogic.swift"]),
        .testTarget(name: "DamovesLogicTests", dependencies: ["DamovesLogic"], path: "Tests")
    ]
)
