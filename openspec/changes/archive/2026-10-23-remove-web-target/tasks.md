# Tasks

## 1. Remove the web target

- [x] 1.1 Delete `webApp/`, `shared/src@wasmJs/` and `shared/test@wasmJs/`, remove `webApp` from `project.yaml` and `wasmJs` from `shared/module.yaml`, and verify `grep -rniE "wasm|webApp" . --exclude-dir=build --exclude-dir=.git --exclude-dir=archive --exclude-dir=openspec` finds nothing outside the README
- [x] 1.2 Update `README.md` (target list, `webApp` bullet, web run command, Kotlin/Wasm link) and verify the same grep finds no matches

## 2. Spec and verification

- [x] 2.1 Confirm `openspec/changes/remove-web-target/specs/app-theming/spec.md` matches the final behavior and verify `openspec validate remove-web-target --strict` passes
- [x] 2.2 Run `./kotlin build` and `./kotlin test` and verify both succeed for android, ios and jvm with no wasm targets listed
