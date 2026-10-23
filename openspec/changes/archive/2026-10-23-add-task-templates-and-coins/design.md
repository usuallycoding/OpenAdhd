# Design

## Context

`shared` is an Amper `kmp/lib` targeting jvm, android, iosArm64 and iosSimulatorArm64 (web was removed). Today it holds the themed shell: `App()` with a centered `CenterAlignedTopAppBar` and a theme dropdown, `ThemeMode`, and a `Greeting`/`Platform` demo. There is no domain model, persistence or navigation. `lifecycle-viewmodel-compose` and `lifecycle-runtime-compose` are already dependencies. Platform code uses `expect`/`actual` with per-target `src@<platform>` folders. See proposal.md for scope; later changes (rewards, crits, Wall of Awful, randomizer, streaks, loans) must be able to build on this without reworking it.

## Goals / Non-Goals

**Goals:**
- A small layered core: pure domain and economy rules, repository interfaces, and a persistent implementation, with Compose UI on top.
- An append-only ledger from day one, so later bonuses and spends are just new entry types.
- Economy logic that is pure and unit-testable on every target.

**Non-Goals:**
- Reward templates, shop, mystery rolls, crits, Wall of Awful, randomizer, streaks, loans, sync, accounts.
- Carry-over of unfinished entries to later days.
- Undoing a completion.
- A navigation library or dependency-injection framework.

## Decisions

- **Layers and packages** under `com.usuallycoding.openadhd`: `domain` (models, economy rules, repository interfaces), `data` (repository implementations), `ui` (screens, view models, existing `ui.theme`). The domain layer has no Compose or platform imports.
- **Models.**
  - `TaskTemplate(id, name, tier, energy)`.
  - `PlannedTask(id, date, name, tier, energy, templateId?, completedAt?)`. It snapshots the template's fields when assigned, so editing or deleting a template never changes history. `templateId` is a plain reference with no integrity constraint.
  - `LedgerEntry(id, amount, timestamp, reason, refId?)`, where `reason` is a closed set starting with `TaskCompleted`.
  - `Tier` is a four-value enum (levels 1-4) and `Energy` is Low/Medium/High.
  - Ids are random UUID strings; amounts are `Int`.
- **Economy as pure functions.** A `CoinRules` object maps tier to base reward (5/10/20/40) in one place. Later crit and Wall of Awful bonuses become additional ledger entries produced by new rules; the base function is not rewritten. Randomness is not needed in this slice, but rules that need it later take an injected `Random`.
- **Atomic, idempotent completion.** A single repository operation completes a `PlannedTask` and appends its ledger entry in one transaction, and does nothing if the entry is already completed. This satisfies the "only once" requirement even if the UI double-fires. Balance is derived as the sum of ledger amounts (queried or folded), never stored separately.
- **Time.** An injected clock supplies "now"; "today" is the clock's local date in the system time zone. The Today view observes the current date so it refreshes when the day rolls over while the app is open. Uses `kotlinx-datetime` for `LocalDate`/`Instant`.
- **Persistence: interfaces first, library chosen by a spike.** Repository interfaces (`TemplateRepository`, `PlannerRepository`, `LedgerRepository`, or one combined store where a transaction is needed) live in `domain`. An in-memory implementation ships for tests and previews. For the persistent implementation, a first task is a short spike that must work on android, ios and jvm with this Kotlin Toolchain:
  1. Prefer an embedded SQL database (SQLDelight, or Room KMP) because the ledger and per-day queries fit SQL and need real transactions, **if** the toolchain supports its code generation without hand-written Gradle.
  2. Otherwise fall back to a JSON file store (`kotlinx-serialization` plus file I/O per platform) behind the same interfaces, with writes done atomically (write temp file, then rename).
  The spike's outcome changes only the data-layer implementation tasks, not the specs or the other layers.
  **Spike outcome:** Room KMP (2.8.4) with the bundled SQLite driver works. The toolchain's built-in `kotlin.ksp.processors` setting runs the Room compiler for android, jvm and both iOS targets, and a write/reopen/read round trip passes on jvm. Room is therefore the persistence library, and the JSON fallback is not needed.
  Database location is decided per platform: each target's source set provides its own factory function that builds the Room database from a file path (Android uses the `Context` database path, iOS the documents directory, desktop an app-data folder under the user's home). These are plain per-platform functions called from each app's entry point, not `expect`/`actual`, because their parameters differ (Android needs a `Context`).
- **State holders.** One view model each for Templates and Today, exposing `StateFlow` UI state and event functions, built on the existing `lifecycle-viewmodel-compose`. A single shared "wallet" state feeds the topbar balance.
- **Wiring.** A small app container constructed per platform entry point and provided to `App()` (no DI framework). On Android it is created from the application context; desktop and iOS create theirs directly.
- **Navigation.** Plain state in `App()` selecting between a Today screen and a Templates screen, switched by a Material `NavigationBar`; no navigation library. Adding to today is a "pick a template" sheet/dialog from Today; creating and editing templates uses a dialog or sheet from Templates.
- **Topbar.** Keeps the centered "OpenAdhd" title and theme dropdown; the coin balance appears as an action beside the theme chooser. This extends the screen content without changing the existing `app-topbar` requirements.
- **Coin values (assumption).** 5/10/20/40 is a doubling curve chosen as a reasonable default; it lives in one function and is expected to be tuned.

## Risks / Trade-offs

- [The toolchain may not support SQLDelight/Room code generation] → spike first; JSON file fallback behind the same interfaces keeps the slice deliverable.
- [JSON file store gets slow or racy as the ledger grows] → acceptable for one user on one device; revisit with a database when the ledger or queries need it. Atomic file replacement prevents torn writes.
- [Snapshotting template fields duplicates data] → intentional; it keeps history stable and avoids foreign-key coupling.
- [Day rollover while the app stays open] → the Today view observes the date; covered by a view-model test with a fake clock.
- [Time-zone or clock changes could shift "today"] → "today" is always derived from the current clock and zone; past entries keep their stored date. Accepted.
- [Unfinished entries are stranded when the day ends] → intended for now; carry-over and "Wall of Awful" age tracking are a later change that will read these stored dates.
- [Android Context needed for storage] → handled by the per-platform container and storage-location hook.

## Open Questions

- Final tier-to-coin values (5/10/20/40) may be retuned; changing them touches one function and its tests.
