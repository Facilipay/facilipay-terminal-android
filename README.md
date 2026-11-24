# Facilipay Terminal SDK for Android

Run gift card operations in Facilipay from your own Android app on a Facilipay terminal: top-ups
(paid by your app, or charged to the customer's card by Facilipay), redemptions and balance checks.

This repository holds:

| | |
|---|---|
| [`terminal-sdk/`](terminal-sdk) | The library, `io.facilipay:terminal-sdk` |
| [`sample/`](sample) | A small app that uses every part of it |
| [`docs/`](docs) | The guide, published at [developers.facilipay.io/terminal](https://developers.facilipay.io/terminal/) |
| [`protocol/`](protocol) | The intent protocol as one file, which the library and Facilipay are both tested against |

**Read the guide first:** [developers.facilipay.io/terminal](https://developers.facilipay.io/terminal/).

## Quick start

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Facilipay's repository, used only for io.facilipay artifacts.
        exclusiveContent {
            forRepository { maven("https://developers.facilipay.io/maven") }
            filter { includeGroup("io.facilipay") }
        }
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

No manifest changes are needed: the SDK declares Facilipay's permission for you. It works in any
project with `minSdk` 21 or higher and depends only on `androidx.activity`.

Can't add an Android library (Flutter, React Native, Xamarin)? Use the
[intent protocol](https://developers.facilipay.io/terminal/reference/intent-protocol.html) directly:
it is what the SDK wraps, and needs nothing from us.

## Checking what you download

Each published version has a build provenance attestation: see [SECURITY.md](SECURITY.md) for the
`gh attestation verify` command.

## Run the sample

You need a Facilipay terminal with app integration switched on (ask
[support@facilipay.io](mailto:support@facilipay.io)), and Facilipay installed **before** the sample.

The sample builds with Android Gradle Plugin 9.3, Gradle 9.6 (through the wrapper) and JDK 17 or
later. AGP 9 compiles Kotlin itself; on AGP 8, add the `org.jetbrains.kotlin.android` plugin to
`sample/build.gradle.kts`. Android Studio writes `local.properties` (your SDK path) when you open
the project; on the command line, create it with `sdk.dir=/path/to/Android/sdk`.

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

### What's where in the sample

| File | Shows |
|---|---|
| [`MainActivity.kt`](sample/src/main/java/io/facilipay/terminal/sample/MainActivity.kt) | Checking availability, registering for the result, building and launching each request |
| [`ResultText.kt`](sample/src/main/java/io/facilipay/terminal/sample/ResultText.kt) | Reading every result type and its next step |
| [`PendingRequests.kt`](sample/src/main/java/io/facilipay/terminal/sample/PendingRequests.kt) | Keeping a request until its result is final, so the same id can be sent again |
| [`JavaExampleActivity.java`](sample/src/main/java/io/facilipay/terminal/sample/JavaExampleActivity.java) | The same calls in Java |

The code in the guide is taken from these files, so every snippet is part of an app that builds.
Names a snippet doesn't define, such as `amountMinor()` or `pendingRequests`, are this app's own
helpers; each snippet says what to replace them with.

## Building from source

The sample always uses the library as a published artifact, the way your app would. To run it
against the library in this repository, publish it to a local folder first:

```bash
./gradlew :terminal-sdk:publishReleasePublicationToTerminalSdkRepository -PterminalSdkVersion=1.0.0-local
./gradlew :sample:installDebug -PfacilipayMaven="file://$PWD/terminal-sdk/build/maven-repo" -PterminalSdkVersion=1.0.0-local
```

`-PfacilipayMaven=<url or path>` picks the Maven repository (for example the staging site,
`https://dev-developers.facilipay.io/maven`) and `-PterminalSdkVersion=<version>` the version.

`scripts/build.sh 0.0.0-pr` runs everything CI runs: the library's tests and lint, the local
publish, the sample against it, and the guide in strict mode (it needs
`pip install -r docs/requirements.txt`).

## Changing the protocol

[`protocol/gift-intent-protocol.properties`](protocol/gift-intent-protocol.properties) lists every
name and value the library and Facilipay share. The library's `GiftProtocolContractTest` checks the
library against it both ways, and Facilipay's app checks itself against a copy pinned to a commit
of this repository. So a change lands here first (the file, the library and the guide), then in
Facilipay. The file only grows: older Facilipay versions on terminals may still send a value, so
removing one needs a new protocol version.

## Releasing

For maintainers. Publishing runs in [`publish.yml`](.github/workflows/publish.yml):

- **Staging:** run the **Publish** workflow on `main` with a release candidate, such as
  `1.0.0-rc.1`. It goes to [dev-developers.facilipay.io/terminal](https://dev-developers.facilipay.io/terminal/).
- **Production:** push a tag `v<x.y.z>` on a commit on `main`, such as `v1.0.0`. A reviewer
  approves the `production` deployment, and it goes to
  [developers.facilipay.io/terminal](https://developers.facilipay.io/terminal/).

A published version is never replaced: a fix needs a new version.

## Licence

Released under the [MIT License](LICENSE).

The Facilipay name and logo are trademarks of Facilipay and aren't covered by the licence.
