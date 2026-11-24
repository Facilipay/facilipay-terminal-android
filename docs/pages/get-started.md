# Get started

This page takes your app from nothing to its first gift card request in five steps.

## Before you begin

You need:

- **A Facilipay terminal** with Facilipay installed and a merchant logged in. For development, ask
  Facilipay for a test terminal on our staging environment (see
  [Test your integration](testing.md)).
- **App integration switched on** for that terminal. Facilipay support switches it on for you:
  email [support@facilipay.io](mailto:support@facilipay.io) with the terminal's serial number.
- **An Android app** with `minSdk` 21 or higher, installed on the same terminal.

!!! important "Install Facilipay first"
    Android grants your app Facilipay's permission only if Facilipay is already installed when your
    app is installed. If your app was installed first, reinstall or update it. Step 2 shows how to
    detect this.

## Step 1: Add the SDK

Add Facilipay's Maven repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        exclusiveContent {
            forRepository { maven("https://developers.facilipay.io/maven") }
            filter { includeGroup("io.facilipay") }
        }
    }
}
```

`exclusiveContent` makes Facilipay's repository the only place Gradle looks for `io.facilipay`
artifacts, and the only thing it looks for there.

Then add the dependency to your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.facilipay:terminal-sdk:1.0.0")
}
```

??? note "Using Groovy build files?"
    In `settings.gradle`:

    ```groovy
    dependencyResolutionManagement {
        repositories {
            google()
            mavenCentral()
            exclusiveContent {
                forRepository { maven { url "https://developers.facilipay.io/maven" } }
                filter { includeGroup "io.facilipay" }
            }
        }
    }
    ```

    In your app's `build.gradle`:

    ```groovy
    dependencies {
        implementation "io.facilipay:terminal-sdk:1.0.0"
    }
    ```

You don't need to change your `AndroidManifest.xml`. The SDK adds Facilipay's permission and the
package visibility entry for you.

## Step 2: Check that Facilipay is available

Before you show a gift card option, check that your app can reach Facilipay:

=== "Kotlin"

    ```kotlin
    --8<-- "sample/src/main/java/io/facilipay/terminal/sample/MainActivity.kt:availability"
    ```

=== "Java"

    ```java
    --8<-- "sample/src/main/java/io/facilipay/terminal/sample/JavaExampleActivity.java:availability"
    ```

| `Availability` | Meaning | What to do |
|---|---|---|
| `READY` | Your app can send requests. | Continue. |
| `NOT_INSTALLED` | Facilipay isn't on this device. | Hide the gift option. |
| `NOT_SUPPORTED` | This Facilipay version is too old. | Ask the merchant to update Facilipay. |
| `PERMISSION_MISSING` | Your app was installed before Facilipay. | Reinstall or update your app. |

`availability()` can't tell whether app integration is switched on for the terminal. If it isn't,
your first request returns `Failed` with the reason `NOT_ENABLED`.

## Step 3: Register for the result

Register a launcher in your activity or fragment, as you would for any
[activity result](https://developer.android.com/training/basics/intents/result). Like every
activity result launcher, register it when the activity or fragment is created, not in a click
listener. It needs an AndroidX `ComponentActivity` (such as `AppCompatActivity`) or an AndroidX
`Fragment`; from a plain `android.app.Activity`, see
[Without the contract](reference/library.md#without-the-contract).

Everything you need is in the `io.facilipay.terminal` package:

```kotlin
import io.facilipay.terminal.Availability
import io.facilipay.terminal.FacilipayGift
import io.facilipay.terminal.GiftRequest
import io.facilipay.terminal.GiftResult
import io.facilipay.terminal.NextStep
```

=== "Kotlin"

    ```kotlin
    --8<-- "sample/src/main/java/io/facilipay/terminal/sample/MainActivity.kt:register"
    ```

=== "Java"

    ```java
    --8<-- "sample/src/main/java/io/facilipay/terminal/sample/JavaExampleActivity.java:register"
    ```

## Step 4: Send a request

Create a `GiftRequest` and launch it. Every request needs a **request id**: a key you choose for
this operation, such as your order number plus a suffix.

=== "Kotlin"

    ```kotlin
    --8<-- "sample/src/main/java/io/facilipay/terminal/sample/MainActivity.kt:balance"
    ```

=== "Java"

    ```java
    --8<-- "sample/src/main/java/io/facilipay/terminal/sample/JavaExampleActivity.java:balance"
    ```

Launching can throw if Facilipay can't be reached, so handle that as well:

=== "Kotlin"

    ```kotlin
    --8<-- "sample/src/main/java/io/facilipay/terminal/sample/MainActivity.kt:launch"
    ```

=== "Java"

    ```java
    --8<-- "sample/src/main/java/io/facilipay/terminal/sample/JavaExampleActivity.java:launch"
    ```

Facilipay comes to the front and asks the merchant for the gift card. Your app comes back when the
merchant is done.

!!! note "Keep the request until it's final"
    Save the request with your order **before** you launch it. If the result isn't final, or your
    app is closed before it arrives, you send the same request again (same id) to get the result.
    Facilipay never runs the same request id twice. [Save the request](results.md#save-the-request)
    says what to store and when to forget it.

## Step 5: Handle the result

Your callback receives a `GiftResult`. Check its type for what happened, and its `nextStep` for what
your app does now. For example, to describe it on screen:

=== "Kotlin"

    ```kotlin
    --8<-- "sample/src/main/java/io/facilipay/terminal/sample/ResultText.kt:handle-result"
    ```

| Result | Meaning |
|---|---|
| `Approved` | Done. `giftCard` has the amount and the new balance. |
| `Cancelled` | Cancelled on the terminal. Nothing was posted or charged. |
| `Declined` | The customer's card was declined. Nothing was posted or charged. |
| `Failed` | Nothing was posted or charged. `reason` says why. |
| `PaidNotCredited` | The customer's card was charged, but the gift card wasn't loaded. The merchant refunds it. |
| `Unknown` | It may have been posted. Don't start a new request for this purchase yet. |

[Handle results](results.md) explains each one and every `nextStep`.

## Next steps

- [Top up a gift card](gift/top-up.md), including a top-up where Facilipay charges the customer's
  card.
- [Redeem from a gift card](gift/redeem.md).
- [Test your integration](testing.md) before you release it.
