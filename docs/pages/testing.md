# Test your integration

Test against Facilipay's staging environment before you release your app. Staging uses test gift
cards and a test payment gateway, so no real money moves.

## Set up

1. **Get a staging terminal.** Ask Facilipay at [support@facilipay.io](mailto:support@facilipay.io)
   for a terminal set up for staging, with app integration switched on, a test merchant login and
   a test gift card.
2. **Install your app after Facilipay** (see [Get started](get-started.md#before-you-begin)).
3. **Optional: run the sample app.** The
   [sample app](https://github.com/Facilipay/facilipay-terminal-android) runs every operation and
   shows each result, which is a quick way to see what Facilipay returns before you write your own
   handling.

## What to test

Run each of these and check that your app does the right thing:

| Case | How | Expected result |
|---|---|---|
| Balance | Check a balance | `Approved`, with the balance |
| Top-up, already paid | Top up with `ALREADY_COLLECTED` | `Approved`, balance up by the amount |
| Top-up, card charged | Top up with `CHARGE_CARD`, tap a test card | `Approved`, with `cardPayment.reference` |
| Card declined | Top up with `CHARGE_CARD` for an amount ending in `.01` (for example €5.01) | `Declined`, balance unchanged |
| Redeem | Redeem less than the balance | `Approved`, balance down by the amount |
| Not enough balance | Redeem more than the balance | `Failed`, `INSUFFICIENT_BALANCE` |
| Partial redeem | Redeem more than the balance with `allowPartial = true` | `Approved`, `isPartial = true` |
| Merchant cancels | Press Cancel on Facilipay's card search screen | `Cancelled` |
| Re-send | Send an approved request again with the same id | The same `Approved`, nothing posted twice |
| Not logged in | Log out of Facilipay, then send a request | `Failed`, `NOT_LOGGED_IN` |

!!! note "Test amounts"
    On the staging payment gateway, a card payment for an amount ending in `.01` is declined. Any
    other amount is approved with a test card.

## Logging

Log every result with its `requestId` and `responseCode` (`GiftResult.toString()` includes both). If
you contact Facilipay support about a request, send those along with the time and the terminal's
serial number.
