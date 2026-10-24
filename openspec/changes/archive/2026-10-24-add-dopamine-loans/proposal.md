# Proposal

## Why

Today the shop is a hard wall: a reward costing 50 cannot be redeemed with 30 coins, however close that is or however much the day's effort has already earned. For someone who runs on motivation in the moment, "you can have this after you do a bit more" is easier to follow than "come back later". Letting the user borrow against coins they will earn, within a limit, gives flexibility without guilt: no interest, no deadline, and every coin earned just pays the debt down.

## What Changes

- **Borrowing:** a redeem or a mystery roll is allowed when the balance after paying would be no lower than -500. The shortfall is the loan; there is no separate loan record, interest, fee or due date. Every coin earned afterwards raises the balance, which pays the debt back automatically.
- **The limit is 500 coins** of debt, fixed in the app (not a setting). A reward or roll that would take the balance below -500 stays refused.
- **Shop:** a reward or roll that needs borrowing stays enabled. "N more coins needed" appears only when the item is beyond the limit, and N is how much beyond.
- **Confirmation:** the redeem and roll confirmations add a line when the purchase puts the balance below zero, for example "This puts you 30 coins in debt."
- **Top bar:** a negative balance is shown in the error colour so the debt is visible.
- **Undoing a completion** can still take the balance below -500, as a correction rather than a spend; spending is then refused until the balance is back within the limit.
- No database change: a purchase on credit is the same redemption and ledger entry as any other.

## Capabilities

### New Capabilities

None.

### Modified Capabilities
- `coin-economy`: spending may borrow up to the debt limit (replacing the rule that a spend can never exceed the balance), and a negative balance is shown in a warning colour.
- `reward-shop`: affordability takes the debt limit into account, and the confirmation of a redemption into debt says how much debt it leaves.
- `mystery-rolls`: when a roll is refused, what the roll options show, and the confirmation for a roll into debt.

## Impact

- `shared` domain: a debt limit constant in `CoinRules` and a single "can pay" check used by `redeem`, `mysteryRoll` and the shop models; both stores (`StoreLogic`, Room DAO) switch their balance guard to it.
- UI: shop affordability and "coins needed" wording, the two confirmation dialogs, the top bar colour.
- Tests: contract tests for both stores (borrow, the limit exactly, beyond the limit, rolling on credit, earning pays it back), shop model tests, UI tests; existing tests that assume a short balance refuses a purchase change.
- Not changed: the ledger, completion and undo rules, and the database (still version 5).
- Out of scope: interest or fees, repayment schedules or reminders, a user-set limit, and any lock-out while in debt.
