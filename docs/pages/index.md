# Facilipay Terminal SDK

Use the Facilipay Terminal SDK to run gift card operations in Facilipay from your own Android app
on a Facilipay terminal. Your app asks, Facilipay handles the gift card and any card payment on the
terminal, and your app gets a clear result back.

<div class="grid cards" markdown>

- **[Get started](get-started.md)**: add the SDK and make your first request in five steps.
- **[Handle results](results.md)**: what each result means and what your app does next.
- **[Sample app](https://github.com/Facilipay/facilipay-terminal-android)**: a complete app that
  runs every operation.

</div>

## What you can do

| Operation | What happens | Guide |
|---|---|---|
| **Top up** a gift card | Loads money onto a gift card. Your app has already taken the payment, or Facilipay charges the customer's card on the terminal. | [Top up a gift card](gift/top-up.md) |
| **Redeem** from a gift card | Takes money off a gift card, for example to pay for an order. | [Redeem from a gift card](gift/redeem.md) |
| **Check a balance** | Reads what a gift card holds. Nothing is posted. | [Check a balance](gift/balance.md) |

More payment types will be added to the same SDK.

## How it works

1. Your app launches a request with the SDK, for example "top up €25.00".
2. Facilipay comes to the front. The merchant presents the customer's gift card, and, if Facilipay
   is taking the payment, the customer pays by card on the terminal.
3. When the merchant is done, your app comes back to the front and receives the result: approved,
   cancelled, declined, failed, or not known yet. Each result tells your app what to do next.

Your app never handles gift card numbers or payment card data. Facilipay shows its own screens,
prints its own receipts, and returns what your app needs to record the operation.

```kotlin
// One launcher in your activity or fragment.
private val gift = registerForActivityResult(FacilipayGift.Contract()) { result ->
    // Approved, Cancelled, Declined, Failed, PaidNotCredited or Unknown.
}

// Then, for example, when the customer asks for their balance. Every new operation gets a new
// request id: re-using one returns the first request's result.
gift.launch(GiftRequest.balance(requestId = UUID.randomUUID().toString()))
```

## Requirements

- A Facilipay terminal (PAX, such as the A920 Pro) with Facilipay installed and a merchant logged
  in.
- App integration switched on for the terminal. Ask Facilipay support at
  [support@facilipay.io](mailto:support@facilipay.io).
- Your app installed on the same terminal, with `minSdk` 21 or higher.

The SDK is a small Android library (`io.facilipay:terminal-sdk`). It works from Kotlin and Java, and
its only dependency is AndroidX Activity.

!!! tip "Not using Kotlin or Java?"
    Frameworks such as Flutter or React Native can call Facilipay through its Android intent
    directly. See the [intent protocol](reference/intent-protocol.md).
