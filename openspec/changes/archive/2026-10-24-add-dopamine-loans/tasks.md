# Tasks

## 1. Rule and stores

- [x] 1.1 Add `DebtLimit`, `canPay`, `coinsNeeded` and `debtAfter` to `CoinRules` with unit tests at the boundaries (positive balance, exactly at the limit, one beyond, already below the limit), and switch `StoreLogic.redeem`, `StoreLogic.mysteryRoll` and the Room DAO redeem and roll transactions to `canPay`
- [x] 1.2 Update the store contract tests: a redeem and a roll on credit are recorded as usual with the balance going negative, the exact limit is allowed, one coin beyond is refused with nothing recorded, earning pays the debt back, a correction by unchecking can go below the limit and then spending is refused; change the existing tests that expect a short balance to refuse; verify they pass against `InMemoryStore` and `RoomStore`

## 2. Shop

- [x] 2.1 Make `buildShelves` and the roll options use `canPay` and the new `coinsNeeded`, and add the debt line to `ConfirmRedeemDialog` and `ConfirmRollDialog` ("This puts you N coins in debt." only when the balance after paying is below zero); add shop model tests for the borrowing, limit and beyond-limit cases and the wording
- [x] 2.2 Colour the top bar balance with the error colour while it is below zero

## 3. UI tests

- [x] 3.1 Update the existing UI tests that assume a short balance disables a reward or roll, and add tests for: redeeming a reward on credit and the balance showing negative, the confirmation naming the debt (and not naming it when there is none), "more coins needed" appearing only beyond the limit, rolling on credit, and the negative balance being shown after the purchase

## 4. Wrap-up

- [x] 4.1 Run `./kotlin build` and `./kotlin test` for all targets and verify both succeed
- [x] 4.2 Verify on desktop: buy a reward you cannot fully afford and read the debt line in the confirmation, see the negative red balance, earn coins and watch it climb back to positive, roll on credit, and try a purchase that would go past -500
