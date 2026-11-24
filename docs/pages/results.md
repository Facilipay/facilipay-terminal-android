# Handle results

Every request ends with one `GiftResult`. Its **type** says what happened, and its **`nextStep`**
says what your app does now. If you follow `nextStep`, your app never posts twice and never loses
track of a customer's money.

## Result types

| Type | What happened | Extra details | `nextStep` |
|---|---|---|---|
| `Approved` | The operation completed. | `giftCard`: amount, balance, masked card number. `isPartial` for a partial redeem. | `DONE` |
| `Cancelled` | Cancelled on the terminal. Nothing was posted or charged. | `reason`: `ON_TERMINAL`, `BY_ANOTHER_REQUEST`, `TIMED_OUT`, `OTHER` | `START_NEW_REQUEST` |
| `Declined` | The customer's card was declined (top-up where Facilipay charges the card). Nothing was posted or charged. | `cardPayment` | `START_NEW_REQUEST` |
| `Failed` | Nothing was posted or charged. | `reason`, see [below](#failure-reasons) | depends on `reason` |
| `PaidNotCredited` | The customer's card was charged, but the gift card wasn't loaded. | `cardPayment.reference` to refund | `REFUND_CARD_PAYMENT` |
| `Unknown` | It may have been posted. | `cardPayment`, when Facilipay was charging the card | `CHECK_PAYMENT_WITH_MERCHANT` when `cardPayment.status` is `UNKNOWN`, otherwise `RESEND_SAME_REQUEST_ID` |

Every result also has `requestId`, `responseCode` (Facilipay's two-character code, see
[Response codes](reference/response-codes.md)) and `message`, which are useful in your logs. They can
be `null` only when Facilipay returned no data at all, which is always an `Unknown`.
`GiftResult.toString()` is safe to log: it holds no receipt text or authorisation codes, and card
numbers only in masked form.

Whenever `cardPayment` is set, show or store `cardPayment.reference`: the merchant needs it to check
or refund the payment.

## Next steps

| `nextStep` | What your app does |
|---|---|
| `DONE` | Nothing. The request is finished. |
| `START_NEW_REQUEST` | Nothing was posted or charged. If the customer wants to try again, send a new request with a **new** request id. |
| `RESEND_SAME_REQUEST_ID` | The outcome isn't final yet. Later (for example when your screen is back, or the merchant says the terminal is free), send the **same** request again to get the final result. Never use a new id for this purchase meanwhile. |
| `FIX_THEN_RETRY` | Something needs fixing first, such as the merchant logging in or app integration being switched on. `reason` says what. Then send a new request. |
| `REFUND_CARD_PAYMENT` | The customer paid by card but the gift card wasn't loaded. The merchant refunds the card payment on the terminal, using `cardPayment.reference`. |
| `CHECK_PAYMENT_WITH_MERCHANT` | The customer's card **may** have been charged. Don't take another payment for this purchase until the merchant has checked `cardPayment.reference`. |

## Failure reasons

| `reason` | Cause | `nextStep` |
|---|---|---|
| `NOT_ENABLED` | App integration isn't switched on for this terminal. | `FIX_THEN_RETRY`: contact [support@facilipay.io](mailto:support@facilipay.io). |
| `NOT_LOGGED_IN` | Nobody is logged in to Facilipay. | `FIX_THEN_RETRY`: the merchant logs in. |
| `BUSY` | Something else is running on the terminal, possibly your own earlier request with the same id. | `RESEND_SAME_REQUEST_ID` once your screen is back. |
| `DUPLICATE_REQUEST_ID` | The request id was already used for a different operation or amount. That earlier request may have posted. | `FIX_THEN_RETRY` with a new id for a new purchase. |
| `INVALID_REQUEST` | Facilipay refused the request, for example for a currency that isn't the merchant's. | `FIX_THEN_RETRY` |
| `INSUFFICIENT_BALANCE` | The gift card holds less than the amount. | `START_NEW_REQUEST` |
| `CARD_EXPIRED`, `CARD_NOT_FOUND`, `CARD_NOT_LINKED` | The gift card can't be used here. | `START_NEW_REQUEST` |
| `NOT_COMPLETED` | The request never completed, and nothing was posted. | `START_NEW_REQUEST` |
| `OTHER` | Any other code: see `responseCode` and `message`. | `START_NEW_REQUEST` |

## Request ids

The request id is what makes a request safe to repeat:

- **Choose one per operation**, such as `"order-4711-load"` or a random UUID: 1 to 128 characters,
  unique among your app's requests.
- **Sending the same request again never runs the operation twice.** If the first request has
  finished, you get its result straight back, without any screens. If it's still running, you get
  `Failed` with the reason `BUSY`.
- **"The same request" means the same id, operation, amount and payment** (`TopUpPayment`). The
  same id with a different operation, amount or payment returns `Failed` with the reason
  `DUPLICATE_REQUEST_ID`.
- Ids belong to your app: another app's ids never clash with yours.

## Save the request

Save each request **before** you launch it, and keep it until its result is final. Then, if your
app is closed mid-request, or a result says `RESEND_SAME_REQUEST_ID`, you can send exactly the same
request again to collect its result.

- **What to save:** the request's `operation`, `requestId`, `amountMinor`, `topUpPayment`,
  `allowPartial` and `currency`. Rebuild it later with the matching `GiftRequest` factory. The
  sample's [`PendingRequests.kt`](https://github.com/Facilipay/facilipay-terminal-android/blob/main/sample/src/main/java/io/facilipay/terminal/sample/PendingRequests.kt)
  does this in a few lines; a real app keeps it with its order.
- **Which request a result belongs to:** send one request at a time, and the result always belongs
  to the request you saved. `result.requestId` matches it, except for an `Unknown` with no data,
  where it can be `null`.
- **When to forget it:** when `nextStep` is `DONE`, `START_NEW_REQUEST`, `FIX_THEN_RETRY` or
  `REFUND_CARD_PAYMENT`, or when `launch()` throws (nothing reached Facilipay).
- **When to keep it:** when `nextStep` is `RESEND_SAME_REQUEST_ID` or `CHECK_PAYMENT_WITH_MERCHANT`.
  Send it again later; the answer becomes final once the merchant or Facilipay resolves it. For
  `CHECK_PAYMENT_WITH_MERCHANT`, keep it until the merchant has checked the card payment
  (`cardPayment.reference`), then forget it. If the customer was charged, the merchant refunds the
  payment, because the gift card wasn't loaded.

!!! warning "A logout clears request ids"
    Facilipay remembers request ids until the merchant logs out, or logs in with another account.
    After that, the same id starts a **new** request, and Facilipay opens its screens instead of
    answering at once. Your app can't see a logout, so when you re-send an `Unknown` request:

    - If Facilipay answers at once, without screens, the answer is for your original request.
    - If Facilipay opens its card search screen instead, it no longer knows the request. The
      merchant should **cancel** it, and check the original with Facilipay support, unless you have
      confirmed the original didn't go through. For a top-up where Facilipay charges the card,
      continuing would charge the customer again.

## Unknown

`Unknown` means the request was interrupted after it may have posted, for example because the
terminal lost its connection or Facilipay was closed mid-request. Never post again with a new id
straight away, or the customer could be charged or loaded twice. Instead:

1. Send the **same** request again. If it has resolved since, its final result comes back.
2. If it's still `Unknown`:
    - When `nextStep` is `CHECK_PAYMENT_WITH_MERCHANT`, the merchant checks the card payment
      (`cardPayment.reference`) before anyone takes another payment.
    - Otherwise, check the card before posting again. For example, compare a
      [balance check](gift/balance.md) with the balance before the request.

`Unknown` is also the result when Facilipay returns no data at all, for example if it was closed
while your request was running.

## One request at a time

Launch one request and wait for its result before you launch the next. If two are launched back to
back, one runs and the other returns `BUSY`, and a single launcher only receives the first result.

If Android recreates your activity while a request is open (for example with the developer option
"Don't keep activities"), your callback receives `Failed` with the reason `BUSY` for that request.
The original request keeps running in Facilipay and nothing is lost: once the merchant has finished
on the terminal, send the same request again to collect its result.
