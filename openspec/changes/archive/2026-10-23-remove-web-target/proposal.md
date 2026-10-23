# Proposal

## Why

OpenAdhd will be developed only for mobile (Android, iOS) and desktop (JVM). The web (Kotlin/Wasm) target adds build time, a platform to keep compiling, and constraints on storage and library choices for the upcoming coins and task features, with no planned user.

## What Changes

- Remove the `webApp` module and drop it from `project.yaml`.
- Remove `wasmJs` from the `shared` module's platforms, along with its platform sources (`Platform.wasmJs.kt`, `PlatformColorScheme.wasmJs.kt`) and test (`PlatformWasmJsTest.kt`).
- Update the README: remove web from the target list, the `webApp` bullet, the web run command and the Kotlin/Wasm link.
- Remove "web" from the dynamic-color requirement's list of platforms.
- **BREAKING**: the app no longer builds or runs in a browser.

## Capabilities

### New Capabilities

### Modified Capabilities
- `app-theming`: the "Android dynamic color" requirement no longer lists web among the platforms that use the app's own color schemes.

## Impact

- Deleted: `webApp/`, `shared/src@wasmJs/`, `shared/test@wasmJs/`.
- Edited: `project.yaml`, `shared/module.yaml`, `README.md`.
- No library dependency changes. Android, iOS and desktop behavior is unchanged.
- Out of scope: the coins and task features (`add-task-templates-and-coins`), which will be designed for the remaining targets.
