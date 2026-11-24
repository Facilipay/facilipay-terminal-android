# Library API

Package `io.facilipay.terminal`, artifact `io.facilipay:terminal-sdk`. The generated
[API reference](../api/index.html) lists every class and member.

## `FacilipayGift`

| Member | Description |
|---|---|
| `Contract(targetPackage = FACILIPAY_PACKAGE)` | The `ActivityResultContract<GiftRequest, GiftResult>` for `registerForActivityResult`. |
| `availability(context, targetPackage = FACILIPAY_PACKAGE): Availability` | Whether your app can call Facilipay on this device. |
| `createIntent(request, targetPackage = FACILIPAY_PACKAGE): Intent` | The intent for `startActivityForResult`, if you don't use the contract. |
| `parseResult(resultCode, data): GiftResult` | Reads the result in `onActivityResult`. |
| `permission(targetPackage = FACILIPAY_PACKAGE): String` | The permission name for a Facilipay package. |
| `FACILIPAY_PACKAGE` | `com.facilipay.pax.pos`, the Facilipay app on Facilipay terminals. |

## `GiftRequest`

| Factory | Parameters |
|---|---|
| `topUp(requestId, amountMinor, payment, currency = null)` | `payment`: `TopUpPayment.ALREADY_COLLECTED` or `TopUpPayment.CHARGE_CARD` |
| `redeem(requestId, amountMinor, allowPartial = false, currency = null)` | `allowPartial`: take what the card holds when it holds less |
| `balance(requestId, currency = null)` | |

The request's properties (`operation`, `requestId`, `amountMinor`, `topUpPayment`, `allowPartial`,
`currency`) are readable, so you can save a request and rebuild it with the same factory; see
[Save the request](../results.md#save-the-request).

- `requestId`: 1 to 128 characters; leading and trailing spaces are removed.
- `amountMinor`: 1 to 99,999,999, in minor units (`2500` is €25.00).
- `currency`: optional three-letter ISO 4217 code. Facilipay refuses a request in another currency
  than the merchant's.

The factories throw `IllegalArgumentException` for values Facilipay would refuse.

## `GiftResult`

A sealed class. Every result has:

| Property | Type | Description |
|---|---|---|
| `requestId` | `String?` | Your request id. `null` only on an `Unknown` with no data. |
| `responseCode` | `String?` | Facilipay's two-character code. `null` only on an `Unknown` with no data. |
| `message` | `String?` | A short description of the code. |
| `sequenceNo` | `String?` | Facilipay's reference for the request, once it created one. |
| `cardPayment` | `CardPayment?` | Only for a top-up with `TopUpPayment.CHARGE_CARD` that Facilipay ran; `null` otherwise, including a card top-up Facilipay refused before running anything (such as `NOT_LOGGED_IN`). |
| `nextStep` | `NextStep` | What your app does now. |

`toString()` is safe to log: it holds no receipt text or authorisation codes, and card numbers only
in masked form.

| Subclass | Extra properties |
|---|---|
| `Approved` | `giftCard: GiftCard`, `isPartial: Boolean` |
| `Cancelled` | `reason: CancelReason` |
| `Declined` | `cardPayment: CardPayment` (never null) |
| `Failed` | `reason: FailureReason` |
| `PaidNotCredited` | `cardPayment: CardPayment` (never null) |
| `Unknown` | |

### `GiftCard`

| Property | Type | Description |
|---|---|---|
| `amountMinor` | `Long` | The amount applied: less than asked on a partial redeem, `0` for a balance check. |
| `balanceMinor` | `Long` | The card's balance afterwards. |
| `maskedCardNumber` | `String?` | The gift card, masked, for example `****9874`. |
| `transactionId` | `String?` | The transaction's reference in Facilipay. |
| `authCode` | `String?` | An authorisation code Facilipay generates for the receipt. |
| `receiptText` | `String?` | The receipt text, when returned. |

### `CardPayment`

Only on a top-up with `TopUpPayment.CHARGE_CARD`.

| Property | Type | Description |
|---|---|---|
| `status` | `PaymentStatus` | `NOT_TAKEN`, `APPROVED`, `DECLINED`, `CANCELLED`, `UNKNOWN` |
| `reference` | `String?` | The payment's reference at the payment provider (`TC_` and 13 characters), once a card payment started. The merchant uses it to find or refund the payment. |
| `responseCode` | `String?` | The card reader's two-character response code, once it answered. |
| `authCode` | `String?` | The card issuer's authorisation code, when it returned one. |
| `maskedCardNumber` | `String?` | The payment card, masked, when a card was read. |
| `amountMinor` | `Long?` | What the card was charged, when approved. |
| `receiptText` | `String?` | The card payment's receipt, when approved. |

### Enums

| Enum | Values |
|---|---|
| `NextStep` | `DONE`, `START_NEW_REQUEST`, `RESEND_SAME_REQUEST_ID`, `FIX_THEN_RETRY`, `REFUND_CARD_PAYMENT`, `CHECK_PAYMENT_WITH_MERCHANT` (see [Handle results](../results.md#next-steps)) |
| `FailureReason` | `INVALID_REQUEST`, `NOT_ENABLED`, `NOT_LOGGED_IN`, `BUSY`, `DUPLICATE_REQUEST_ID`, `NOT_COMPLETED`, `INSUFFICIENT_BALANCE`, `CARD_EXPIRED`, `CARD_NOT_FOUND`, `CARD_NOT_LINKED`, `OTHER` |
| `CancelReason` | `ON_TERMINAL`, `BY_ANOTHER_REQUEST`, `TIMED_OUT`, `OTHER` |
| `Availability` | `READY`, `NOT_INSTALLED`, `NOT_SUPPORTED`, `PERMISSION_MISSING` |

## Without the contract

`FacilipayGift.Contract` needs an AndroidX `ComponentActivity` or `Fragment`. From a plain
`android.app.Activity`, start the intent yourself and read the result in `onActivityResult`:

```kotlin
private const val GIFT_REQUEST_CODE = 4711

fun startGift(request: GiftRequest) {
    startActivityForResult(FacilipayGift.createIntent(request), GIFT_REQUEST_CODE)
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == GIFT_REQUEST_CODE) {
        val result = FacilipayGift.parseResult(resultCode, data)
        // Handle it as on "Handle results".
    }
}
```

Here an `Unknown` with no data has a `null` `requestId`: use the request you saved.

## Exceptions

- `GiftRequest` factories throw `IllegalArgumentException` for values Facilipay would refuse.
- Launching throws `ActivityNotFoundException` when Facilipay isn't installed or is too old, and
  `SecurityException` when your app lacks Facilipay's permission. `availability()` tells you in
  advance.
- Nothing else is thrown: every outcome of a request arrives as a `GiftResult`.

## Compatibility

- `minSdk` 21. Any `compileSdk` from 21.
- Java 11 bytecode. Readable by Kotlin 1.9 and later.
- One dependency: `androidx.activity:activity` 1.2.4 or later.
- No reflection, so no ProGuard or R8 rules are needed.
