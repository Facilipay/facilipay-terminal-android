# Check a balance

A balance check reads what the gift card holds. Nothing is posted, so it is always safe to run.

=== "Kotlin"

    ```kotlin
    --8<-- "sample/src/main/java/io/facilipay/terminal/sample/MainActivity.kt:balance"
    ```

=== "Java"

    ```java
    --8<-- "sample/src/main/java/io/facilipay/terminal/sample/JavaExampleActivity.java:balance"
    ```

On `Approved`, `result.giftCard.balanceMinor` is the balance and `result.giftCard.maskedCardNumber`
the card that was read (for example `****9874`). `giftCard.amountMinor` is `0`.

Each balance check still needs its own request id. Sending the same id again returns the first
check's result, not a new balance.
