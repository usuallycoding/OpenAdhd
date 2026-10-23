# Design

## Context

The repo has five Amper modules: `androidApp`, `desktopApp`, `iosApp`, `webApp` and `shared` (`kmp/lib` with jvm, android, iosArm64, iosSimulatorArm64, wasmJs). Wasm-specific code is limited to `Platform.wasmJs.kt`, `PlatformColorScheme.wasmJs.kt` and `PlatformWasmJsTest.kt`. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- Remove every wasm/web artifact so `./kotlin build` and `./kotlin test` pass on the remaining targets.

**Non-Goals:**
- Changing any behavior on android, ios or jvm.
- Reworking the platform `expect`/`actual` structure beyond deleting the wasm actuals.

## Decisions

- **Delete, don't disable.** Remove `webApp` and the wasm source sets outright rather than keeping them out of `project.yaml`. Dead modules would drift and mislead; git history keeps them recoverable.
- **Keep the `expect` declarations.** `getPlatform()` and `platformColorScheme()` stay `expect` with actuals on android, ios and jvm; each remaining target already has one.
- **Spec edit via a MODIFIED delta** on `app-theming`, since removing "web" from a scenario changes a stated requirement.
- **README:** update the title and bullets; the generic Kotlin Multiplatform and Compose Multiplatform links stay.

## Risks / Trade-offs

- [A stale reference to wasm remains somewhere (docs, IDE run configs)] → grep for `wasm` and `webApp` outside `build/` and archives after the removal.
- [Irreversible for users on web] → none exist; accepted, git history keeps the code.
- [Generated Xcode files may change during the verification build] → not part of this change; leave them uncommitted.
