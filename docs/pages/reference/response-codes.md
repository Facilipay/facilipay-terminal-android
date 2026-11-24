# Response codes

`GiftResult.responseCode` (the `RESPONSE_CODE` extra) is a two-character code. The SDK already turns
it into a result type and a reason; this table is for your logs and for support.

## Success

| Code | Result | Meaning |
|---|---|---|
| `00` | `Approved` | Approved. |
| `10` | `Approved`, `isPartial = true` | Partial redeem: the card held less than asked, and that's what was taken. |

## Cancelled

| Code | `CancelReason` | Meaning |
|---|---|---|
| `P3` | `ON_TERMINAL` | The merchant cancelled or left the flow, or the card couldn't be read. |
| `P4` | `BY_ANOTHER_REQUEST` | Another request on the terminal took over. |
| `P5` | `TIMED_OUT` | The terminal timed out waiting for the merchant. |

## Refused before anything ran

| Code | `FailureReason` | Meaning |
|---|---|---|
| `30` | `INVALID_REQUEST` | Malformed request, or an `API_VERSION` this Facilipay doesn't support. |
| `12` | `INVALID_REQUEST` | Missing or unknown operation. |
| `13` | `INVALID_REQUEST` | Invalid amount or currency, or a missing `PAYMENT_COLLECTED`. |
| `58` | `NOT_ENABLED` | App integration isn't switched on for this terminal, or this Facilipay can't charge cards. |
| `27` | `NOT_LOGGED_IN` | Nobody is logged in to Facilipay. |
| `09` | `BUSY` | Another request, or a card payment, is in progress. |
| `94` | `DUPLICATE_REQUEST_ID` | The request id was already used for a different operation or amount. |

## From the gift card

| Code | `FailureReason` | Meaning |
|---|---|---|
| `51` | `INSUFFICIENT_BALANCE` | The card holds less than the amount. |
| `54` | `CARD_EXPIRED` | The card has expired. |
| `56` | `CARD_NOT_FOUND` | The card wasn't found. |
| `14` | `CARD_NOT_LINKED` | The card isn't linked to this merchant. |
| `06` | `OTHER` | A failure before anything was posted. |
| `21` | `NOT_COMPLETED` | The request never completed, and nothing was posted. |

## From the customer's card payment

Only on a top-up with `TopUpPayment.CHARGE_CARD`.

| Code | Result | Meaning |
|---|---|---|
| `05`, `04`, `01` | `Declined` | The card was declined, or the payment was reversed. Nothing was charged. |
| `PC` | `PaidNotCredited` | The card was charged, and the gift card wasn't loaded. The merchant refunds the card payment. |

## Outcome unknown

| Code | Result | Meaning |
|---|---|---|
| `22` | `Unknown` | The request was interrupted and may have posted. See [Unknown](../results.md#unknown). |

Any other code comes from the gift card system, maps to `FailureReason.OTHER`, and means nothing was
posted.
