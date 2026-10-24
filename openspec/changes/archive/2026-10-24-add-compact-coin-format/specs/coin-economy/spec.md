# Spec Delta

## ADDED Requirements

### Requirement: Compact coin amounts
Every coin amount the app displays SHALL be shown as is below 1000. From 1000 it SHALL be shown in thousands with one decimal ("k"), from 100,000 in thousands with no decimal, from 1,000,000 in millions with one decimal ("M") and from 100,000,000 in millions with no decimal. Amounts MUST be truncated, never rounded up, and a zero decimal MUST be dropped. A negative amount keeps its sign.

#### Scenario: Below one thousand
- **WHEN** an amount is 999
- **THEN** it is shown as 999

#### Scenario: Thousands
- **WHEN** an amount is 1000, 1123 or 1999
- **THEN** it is shown as 1k, 1.1k and 1.9k

#### Scenario: Hundred thousands
- **WHEN** an amount is 12,500 or 100,400 or 999,999
- **THEN** it is shown as 12.5k, 100k and 999k

#### Scenario: Millions
- **WHEN** an amount is 1,250,000 or 2,000,000 or 123,456,789
- **THEN** it is shown as 1.2M, 2M and 123M

#### Scenario: Negative amounts
- **WHEN** the balance is -1500
- **THEN** it is shown as -1.5k, in the debt colour

#### Scenario: Where it applies
- **WHEN** the balance is 1123 and a reward costs 1250 coins
- **THEN** the top bar shows 1.1k and the Shop shows the price as 1.2k coins

### Requirement: Entered amounts stay exact
Numbers the user types or edits, and validation messages that quote a limit, SHALL stay exact and MUST NOT use the compact format.

#### Scenario: Editing a price
- **WHEN** a reward's custom price is 1250
- **THEN** the price field shows 1250
