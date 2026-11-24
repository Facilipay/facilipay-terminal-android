# Redeem from a gift card

A redeem takes money off the gift card the merchant presents, for example to pay for an order.

=== "Kotlin"

    ```kotlin
    --8<-- "sample/src/main/java/io/facilipay/terminal/sample/MainActivity.kt:redeem"
    ```

=== "Java"

    ```java
    --8<-- "sample/src/main/java/io/facilipay/terminal/sample/JavaExampleActivity.java:redeem"
    ```

On `Approved`, `result.giftCard.amountMinor` is the amount taken and `result.giftCard.balanceMinor`
what's left on the card.

## When the card holds less than the amount

By default, a redeem for more than the card holds fails with the reason `INSUFFICIENT_BALANCE`, and
nothing is taken.

To take whatever the card holds instead, pass `allowPartial = true`. The result is then `Approved`
with `isPartial = true`, and `giftCard.amountMinor` is what was taken. Your app collects the rest
another way.

```kotlin
GiftRequest.redeem(requestId = "order-4711-redeem", amountMinor = 3_000, allowPartial = true)
```

## Common results

| Result | `reason` | What to do |
|---|---|---|
| `Approved` | | Done. |
| `Failed` | `INSUFFICIENT_BALANCE` | Ask for another card, or another way to pay. |
| `Failed` | `CARD_EXPIRED`, `CARD_NOT_FOUND`, `CARD_NOT_LINKED` | The card can't be used here. |
| `Cancelled` | | The merchant cancelled. Nothing was taken. |
| `Unknown` | | The gift card may have been debited. Send the same request again. See [Handle results](../results.md#unknown). |
