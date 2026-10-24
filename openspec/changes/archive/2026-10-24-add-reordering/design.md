# Design

## Context

Task templates, reward templates and Today entries are all listed with `ORDER BY rowid`, which is insertion order. `InMemoryStore` keeps plain lists with the same effect. Subtasks already have a `position` column, so there is precedent. The Shop groups rewards by tier and keeps list order within a tier, except tier 5, which is sorted by price in `ShopModels`. The picker and the randomizer read `templates` in list order. Room is at version 4.

## Goals / Non-Goals

**Goals**
- Let the user reorder Today's pending entries, task templates and reward templates.
- Persist the order, including across the database upgrade, with no visible change for existing data.
- Keep both stores behind one contract, tested by one shared suite.

**Non-Goals**
- Drag and drop (the same move operation could back it later).
- Reordering subtasks or the Shop directly.
- Remembering the Reorder mode or anything else across restarts.

## Decisions

1. **Explicit Reorder mode with arrows.** A toggle in each screen's top bar, arrows on the rows while it is on. This was chosen over drag handles because it needs no new library, works the same with touch, mouse and keyboard, and cannot fight the tab swipe. The mode is plain UI state in the view model and is never saved.
2. **One store operation per list: set the whole order.** `reorderTemplates(ids)`, `reorderRewards(ids)` and `reorderEntries(date, ids)`. Each takes the ids in the desired order and rewrites positions 0..n in one transaction. Ids not in the list are ignored and ids missing from it keep their relative order after the listed ones, so a stale call cannot lose items. The view model computes the new list from a swap, so the store has no move-up or move-down concept.
3. **`position` columns.** `templates`, `reward_templates` and `planned_tasks` get `position INTEGER NOT NULL DEFAULT 0`. Lists are read with `ORDER BY position, rowid`, so ties fall back to insertion order. Inserts use `COALESCE(MAX(position), -1) + 1` within the table, or within the date for planned tasks. In `InMemoryStore` the list order itself is the order.
4. **Migration 4 to 5.** Three `ALTER TABLE ... ADD COLUMN` statements, each followed by an `UPDATE` that sets `position` to the number of earlier rows by rowid (per date for planned tasks), so existing lists look exactly as before. `exportSchema` handling and `MigrationTestHelper`-style testing follow the earlier migrations.
5. **Completed entries are a display rule, not stored.** The store keeps one position order for a date. The Today list shows pending entries first, then completed ones, each group in position order. The view model's move functions take the pending ids in the displayed order and call `reorderEntries` with them followed by the completed ids, so completed entries keep their relative order and always sit after.
6. **Controls in reorder mode.** The row composables take a `reordering` flag. While it is on they show the arrows (enabled by position) and drop the controls that change data. The toggle is shown only when there are at least two movable items; if the list shrinks below two while the mode is on, the mode ends.
7. **Shop and picker need no change.** They already follow list order; the Shop's tier 5 price sort stays. Specs state it so it stays that way.

## Risks / Trade-offs

- **Completed entries jumping to the bottom** when completed could surprise the user. It is the agreed behavior and is covered by a scenario.
- **Arrows are slow for long moves.** Accepted; drag handles remain a later option on the same operation.
- **Migration mistakes would scramble the user's lists.** Mitigated by a migration test with several rows per table and two dates, and by the user checking their real database before archiving.
- **Position gaps or duplicates** (for example after deleting an item) are harmless because ordering is `position, rowid` and every reorder rewrites positions densely.

## Migration Plan

Room version 5 with `Migration4To5`, registered next to the earlier migrations. No data is dropped. Rollback is not supported, as with earlier migrations; the user backs up `~/.openadhd` before the first run.

## Open Questions

None.
