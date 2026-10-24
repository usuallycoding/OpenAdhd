# Design

## Context

The balance is the sum of all ledger amounts and may already be negative (unchecking a completion). The only thing standing between a short balance and a purchase is a comparison of the balance with the price: `StoreLogic.redeem` and `StoreLogic.mysteryRoll` for the in-memory store, the equivalent checks inside the Room DAO transactions (`redeemReward`, `rollReward`), and `affordable`/`coinsNeeded` in `buildShelves` and the roll options in the shop models. The two confirmation dialogs show only the price. The top bar shows the balance as plain text.

## Goals / Non-Goals

**Goals**
- Allow a redeem or a roll when the balance after paying is no lower than -500, in both stores and in the shop's idea of what is affordable.
- Tell the user when a purchase puts them in debt, before they confirm.
- Make a negative balance visible.

**Non-Goals**
- Interest, fees, repayment plans, reminders, a user-set limit, or any lock-out while in debt.
- A loan record: the debt is just the negative balance.
- Any database change.

## Decisions

1. **One rule, one place.** `CoinRules` gets `DebtLimit = 500` and `canPay(balance, price) = balance - price >= -DebtLimit`, plus `coinsNeeded(balance, price) = max(0, price - balance - DebtLimit)` and `debtAfter(balance, price) = max(0, price - balance)`. The stores, the shop models and the dialogs all call these, so the number exists once. The pure functions are unit-tested at the boundary (exactly at the limit, one beyond).
2. **Stores.** `StoreLogic.redeem` and `mysteryRoll` replace `balance < price` with `!canPay(...)`. The Room DAO transactions already read the balance with `SUM(amount)`; they use the same check. Nothing else about a redemption changes, so a purchase on credit is the usual redemption row and a `-price` ledger entry, and there is no migration (Room stays at version 5).
3. **Shop models.** `affordable = canPay(balance, price)` and `coinsNeeded = coinsNeeded(balance, price)` for rewards and for roll options, so "N more coins needed" appears only beyond the limit and N is the amount beyond it (a 50-coin reward at -480 shows 30). The existing buttons already follow `affordable`.
4. **Confirmation text.** `ConfirmRedeemDialog` and `ConfirmRollDialog` take the current balance and add "This puts you N coins in debt." when `debtAfter` is above zero. The balance is already available to `ShopScreen` through the view model; the confirm button keeps its single-use id behavior unchanged.
5. **Undo is a correction, not a spend.** `uncompleteTask` and `uncompleteSubtask` stay unguarded, so the balance can fall below -500; `canPay` then simply refuses spending until earnings bring it back. This matches the existing "unchecking is always allowed" behavior.
6. **Top bar.** `BalanceChip` colours the number with the theme's error colour when the balance is below zero. It reads the same balance flow it does today.
7. **No hidden state.** Nothing remembers that the user "has a loan": opening the app in debt looks like any other state, and every earning (including a critical-hit bonus) pays it down.

## Risks / Trade-offs

- **Borrowing without feeling it.** With no interest or deadline there is no pressure to earn back; that is the intent (anti-guilt), and the confirmation line plus the red balance are the only nudges.
- **Rolling on credit** can mean paying off a gamble that did not feel worth it; accepted, and the roll confirmation states the debt.
- **Existing tests** that assert a short balance refuses a purchase must change to the new boundary.
- **Big prices:** a tier 5 reward at 900 needs a balance of at least 400, so tier 5 stays within reach only after earning, which is intended.

## Migration Plan

None. No schema or data change; existing users keep their balance and simply gain the ability to borrow.

## Open Questions

None.
