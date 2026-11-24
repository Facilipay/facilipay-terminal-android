# Top up a gift card

A top-up loads money onto the gift card the merchant presents. Choose who takes the customer's
money:

| | Your app has already taken the payment | Facilipay charges the customer's card |
|---|---|---|
| `TopUpPayment` | `ALREADY_COLLECTED` | `CHARGE_CARD` |
| Use it when | Your app took cash, or ran its own card payment | Your app doesn't take payments, or you want one flow on Facilipay's screens |
| On the terminal | Facilipay loads the gift card | Facilipay takes the card payment, then loads the gift card |
| `result.cardPayment` | `null` | The card payment's details |

## Your app has already taken the payment

=== "Kotlin"

    ```kotlin
    --8<-- "sample/src/main/java/io/facilipay/terminal/sample/MainActivity.kt:top-up-paid"
    ```

=== "Java"

    ```java
    --8<-- "sample/src/main/java/io/facilipay/terminal/sample/JavaExampleActivity.java:top-up-paid"
    ```

On the terminal, the merchant presents the gift card and confirms the top-up. On `Approved`,
`result.giftCard.amountMinor` is the amount loaded and `result.giftCard.balanceMinor` the new
balance.

!!! warning "If the top-up doesn't go through"
    Your app has the customer's money, so it must not lose track of the top-up:

    - `Cancelled`, `Failed`: nothing was loaded. Follow `nextStep`: try again (a new request id for
      `START_NEW_REQUEST`, the same one for `RESEND_SAME_REQUEST_ID`), or refund the customer in
      your app.
    - `Unknown`: the card may have been loaded. Send the same request again before you do anything
      else. See [Handle results](../results.md#unknown).

## Facilipay charges the customer's card

=== "Kotlin"

    ```kotlin
    --8<-- "sample/src/main/java/io/facilipay/terminal/sample/MainActivity.kt:top-up-charge"
    ```

=== "Java"

    ```java
    --8<-- "sample/src/main/java/io/facilipay/terminal/sample/JavaExampleActivity.java:top-up-charge"
    ```

On the terminal:

1. The merchant presents the gift card.
2. Facilipay asks the customer to tap, insert or swipe their payment card, for exactly the requested
   amount. There is no tip, surcharge or cashback, and only one card attempt per request.
3. Once the card is approved, Facilipay loads the gift card and shows its success screen.

The result describes both the gift card and the card payment:

| Result | Card payment (`cardPayment.status`) | Gift card | What to do |
|---|---|---|---|
| `Approved` | `APPROVED` | Loaded | Done. Keep `cardPayment.reference` with your order. |
| `Declined` | `DECLINED` | Not loaded | Nothing was charged. The customer can try again (new request id). |
| `Cancelled` | `CANCELLED` or `NOT_TAKEN` | Not loaded | Nothing was charged. |
| `PaidNotCredited` | `APPROVED` | Not loaded | The merchant refunds the card payment (`cardPayment.reference`). |
| `Unknown` | `APPROVED` | Not known yet | Send the same request again later. |
| `Unknown` | `UNKNOWN` | Not loaded | The card **may** have been charged. Don't charge the customer again until the merchant has checked the payment. |

`cardPayment.reference` identifies the payment at Facilipay's payment provider. The merchant needs
it to find or refund the payment, so store it with your order.

### If the gift card can't be loaded after the card was charged

This is rare: for example, the terminal loses its connection right after the payment. Facilipay
keeps the top-up on its retry list, and the merchant chooses:

- **Retry**: Facilipay loads the gift card, and the request ends `Approved`.
- **Remove**: the request ends `PaidNotCredited`, and the merchant refunds the card payment.

While the retry list is on screen, sending the same request again returns `Failed` with the reason
`BUSY`. Facilipay never refunds or voids the card payment by itself.

## Amounts

Amounts are in minor units of the merchant's currency: `2500` is €25.00. The SDK accepts 1 to
99,999,999. To make sure the terminal uses the currency you expect, pass `currency`:

```kotlin
GiftRequest.topUp("order-4711-load", 2_500, GiftRequest.TopUpPayment.CHARGE_CARD, currency = "EUR")
```

Facilipay refuses a request whose currency isn't the merchant's (`Failed`, `INVALID_REQUEST`).
Results carry no currency: amounts are always in the merchant's currency.
