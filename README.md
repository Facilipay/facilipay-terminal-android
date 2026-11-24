# Facilipay Terminal SDK sample

A small Android app that shows how to use the
[Facilipay Terminal SDK](https://developers.facilipay.io/terminal/) to run gift card operations in
Facilipay from your own app on a Facilipay terminal: top-ups (paid by your app, or charged to the
customer's card by Facilipay), redemptions and balance checks.

**Read the guide first:** [developers.facilipay.io/terminal](https://developers.facilipay.io/terminal/).

## Quick start

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://developers.facilipay.io/maven")
    }
}

// app/build.gradle.kts
dependencies {
    implementation("io.facilipay:terminal-sdk:1.0.0")
}
```

```kotlin
private val gift = registerForActivityResult(FacilipayGift.Contract()) { result ->
    when (result) {
        is GiftResult.Approved -> showBalance(result.giftCard.balanceMinor)
        else -> handle(result.nextStep) // See "Handle results" in the guide.
    }
}

gift.launch(GiftRequest.balance(requestId = UUID.randomUUID().toString()))
```

No manifest changes are needed: the SDK declares Facilipay's permission for you.

## Run the sample

You need a Facilipay terminal with app integration switched on (ask
[support@facilipay.io](mailto:support@facilipay.io)), and Facilipay installed **before** the sample.

The sample builds with Android Gradle Plugin 9.3, Gradle 9.6 (through the wrapper) and JDK 17 or
later. AGP 9 compiles Kotlin itself; on AGP 8, add the `org.jetbrains.kotlin.android` plugin to
`sample/build.gradle.kts`. Android Studio writes `local.properties` (your SDK path) when you open
the project; on the command line, create it with `sdk.dir=/path/to/Android/sdk`.

The SDK itself has no such requirements: it works in any project with `minSdk` 21 or higher.

```bash
./gradlew :sample:installDebug
```

The screen shows whether Facilipay is available, a button per operation, and the last result with
what to do next. "Send the last request again" re-sends the last request that isn't final yet, with
the same request id, to show that Facilipay never runs it twice.

Every result is also logged:

```bash
adb logcat -s FacilipaySample
```

## What's where

| File | Shows |
|---|---|
| [`MainActivity.kt`](sample/src/main/java/io/facilipay/terminal/sample/MainActivity.kt) | Checking availability, registering for the result, building and launching each request |
| [`ResultText.kt`](sample/src/main/java/io/facilipay/terminal/sample/ResultText.kt) | Reading every result type and its next step |
| [`PendingRequests.kt`](sample/src/main/java/io/facilipay/terminal/sample/PendingRequests.kt) | Keeping a request until its result is final, so the same id can be sent again |
| [`JavaExampleActivity.java`](sample/src/main/java/io/facilipay/terminal/sample/JavaExampleActivity.java) | The same calls in Java |

The code in the guide is taken from these files, so every snippet is part of an app that builds.
Names a snippet doesn't define, such as `amountMinor()` or `pendingRequests`, are this app's own
helpers; each snippet says what to replace them with.

## Using another build of the SDK

`-PfacilipayMaven=<url or path>` builds against another Maven repository, for example the staging
site (`https://dev-developers.facilipay.io/maven`). `-PterminalSdkVersion=<version>` picks the SDK
version.

## Licence

Released under the [MIT License](LICENSE).

The Facilipay name and logo are trademarks of Facilipay and aren't covered by the licence.
