# Troubleshooting

## `availability()` doesn't return `READY`

| `Availability` | Cause | Fix |
|---|---|---|
| `NOT_INSTALLED` | Facilipay isn't installed on this device. | Use a Facilipay terminal. |
| `NOT_SUPPORTED` | The installed Facilipay is too old for app integration. | The merchant updates Facilipay. |
| `PERMISSION_MISSING` | Your app was installed before Facilipay, so Android didn't grant Facilipay's permission. | Reinstall or update your app. Updating Facilipay later keeps the permission. |

## Launching throws an exception

- **`ActivityNotFoundException`**: Facilipay isn't installed, or is too old. Check
  `availability()` first.
- **`SecurityException`**: your app doesn't hold Facilipay's permission. Reinstall or update your app now
  that Facilipay is installed.

## Every request returns `Failed` with `NOT_ENABLED`

App integration isn't switched on for this terminal. Email
[support@facilipay.io](mailto:support@facilipay.io) with the terminal's serial number.

## A request returns `Failed` with `BUSY`

Something else is running on the terminal:

- **Your own earlier request** with the same id is still open, for example on Facilipay's retry
  list. Facilipay comes to the front on it so the merchant can finish it. When your screen is back,
  send the same request again.
- **Another request**, or a card payment started from Facilipay itself, is in progress. Wait until
  it finishes, then send the same request again.

## My app didn't get a result

Facilipay hands the result back when the merchant leaves its screens: after the success screen
closes, a balance is shown, the merchant cancels, or the terminal times out. If your app was closed
meanwhile, send the same request again when it restarts to collect the result.

If Facilipay itself was closed during the request, your app receives `Unknown`. Send the same
request again; see [Unknown](results.md#unknown).

## Two requests, one result

A launcher delivers one result at a time. Launch one request and wait for its result before you
launch the next. To collect the other request's result, send it again with the same id.

## A re-sent request started again

Facilipay forgets request ids when the merchant logs out, or logs in with another account. After
that, the same id starts a new request. If you were recovering an `Unknown` request, the merchant
should cancel the new one unless you've confirmed the first didn't go through.

## Using another Facilipay build

The SDK targets the Facilipay app on Facilipay terminals (`com.facilipay.pax.pos`). To target
another build, for example a development build Facilipay gave you, pass its package to the contract
and declare its permission:

```kotlin
private val gift = registerForActivityResult(FacilipayGift.Contract("com.example.facilipay.dev")) { … }
```

```xml
<uses-permission android:name="com.example.facilipay.dev.permission.GIFT_INTEGRATION" />
```

Android 11 and later also need a `<queries>` entry for that package if you call `availability()`.

## Questions

**Which currency are amounts in?** Always the merchant's. Results carry no currency, so format them
in the currency your merchant uses, or pass `currency` with each request to make sure it matches.
Facilipay's staging test merchant uses EUR.

**How long does Facilipay stay in front?** Until the merchant finishes: the success screen closes,
a balance is shown, the merchant cancels, or the terminal times out (`Cancelled`, `TIMED_OUT`).
Your activity is in the background meanwhile, so it doesn't need to show anything; when it's back,
your callback has the result.

**Is `GiftResult.toString()` safe to log?** Yes. It holds no receipt text or authorisation codes,
and card numbers only in masked form.

**Can a request throw?** Only when it can't reach Facilipay; see
[Exceptions](reference/library.md#exceptions). Every outcome of a request that reached Facilipay
arrives as a `GiftResult`.
