# Intent protocol

The SDK wraps an Android intent that Facilipay exposes. Use this page if your app can't use the
SDK, for example a Flutter, React Native or Xamarin app that starts Android activities itself.
Otherwise, use the SDK: it builds these intents and reads these results for you.

## The request

Start an activity **for a result** (`startActivityForResult`, or an `ActivityResultLauncher`):

| | Value |
|---|---|
| Action | `com.facilipay.pos.action.GIFT_TRANSACTION` |
| Category | `android.intent.category.DEFAULT` |
| Package | `com.facilipay.pax.pos` |

Your app must hold the permission `com.facilipay.pax.pos.permission.GIFT_INTEGRATION`, and, on
Android 11 and later, declare the package in `<queries>`:

```xml
<uses-permission android:name="com.facilipay.pax.pos.permission.GIFT_INTEGRATION" />

<queries>
    <package android:name="com.facilipay.pax.pos" />
</queries>
```

!!! warning "Always start it for a result"
    Started with `startActivity`, or with `FLAG_ACTIVITY_NEW_TASK`, Facilipay can't tell who sent the
    request. It refuses it without running anything, and your app receives `RESULT_CANCELED` with no
    data at once.

### Request extras

All names start with `com.facilipay.pos.extra.`.

| Extra | Type | `TOPUP` | `REDEEM` | `BALANCE` |
|---|---|---|---|---|
| `REQUEST_ID` | String, 1 to 128 characters | required | required | required |
| `OPERATION` | String: `TOPUP`, `REDEEM`, `BALANCE` | required | required | required |
| `AMOUNT_MINOR` | Long, 1 to 99,999,999 | required | required | must be absent |
| `PAYMENT_COLLECTED` | Boolean | required | | |
| `ALLOW_PARTIAL` | Boolean | | optional | |
| `CURRENCY` | String, ISO 4217 | optional | optional | optional |
| `API_VERSION` | Int, default `1` | optional | optional | optional |

- `PAYMENT_COLLECTED = true`: your app has already taken the payment.
- `PAYMENT_COLLECTED = false`: Facilipay charges the customer's card. This needs `API_VERSION = 2`.
- Send the lowest `API_VERSION` the request needs: `2` only for `PAYMENT_COLLECTED = false`, `1`
  otherwise. A Facilipay that doesn't know the version you send answers `30`.

## The result

`RESULT_OK` when approved, `RESULT_CANCELED` otherwise. Both carry the same extras, all starting
with `com.facilipay.pos.result.`. `RESULT_CANCELED` with no data after the request was running
means the outcome is unknown: treat it as `UNKNOWN`.

| Extra | Type | When |
|---|---|---|
| `STATUS` | String: `APPROVED`, `CANCELLED`, `ERROR`, `UNKNOWN` | always |
| `RESPONSE_CODE` | String, two characters | always; see [Response codes](response-codes.md) |
| `MESSAGE` | String, up to 32 characters | always |
| `REQUEST_ID` | String | your request id, trimmed |
| `SEQUENCE_NO` | String | once Facilipay created the request |
| `TRANSACTION_UID` | String | when approved |
| `AMOUNT_MINOR` | Long | when approved: the amount applied |
| `BALANCE_MINOR` | Long | when approved: the balance afterwards |
| `AUTH_CODE` | String | when approved |
| `MASKED_PAN` | String | when a gift card was read |
| `RECEIPT_TEXT` | String | when approved |

### Card payment extras

Only when `PAYMENT_COLLECTED = false`, on every answer except a refusal that ran nothing:

| Extra | Type | When |
|---|---|---|
| `PAYMENT_STATUS` | String: `NOT_TAKEN`, `APPROVED`, `DECLINED`, `CANCELLED`, `UNKNOWN` | always |
| `PAYMENT_REFERENCE` | String | once a card payment started |
| `PAYMENT_RESPONSE_CODE` | String | once the card reader answered |
| `PAYMENT_AUTH_CODE` | String | when the card issuer returned one |
| `PAYMENT_MASKED_PAN` | String | when a payment card was read |
| `PAYMENT_AMOUNT_MINOR` | Long | when approved |
| `PAYMENT_RECEIPT_TEXT` | String | when approved |

### Reading the status

| `STATUS` | Meaning |
|---|---|
| `APPROVED` | Done (`00`, or `10` for a partial redeem). |
| `CANCELLED` | Nothing was posted or charged. |
| `ERROR` | Nothing was posted or charged, except `PC`: the card was charged and the gift card wasn't loaded, so the merchant refunds it. With `PAYMENT_STATUS = DECLINED`, the customer's card was declined. |
| `UNKNOWN` | May have been posted. Send the same request again; see [Unknown](../results.md#unknown). |

[Handle results](../results.md) applies here too: the SDK's result types map one to one onto these
statuses and codes.

## Versions

| `API_VERSION` | Adds |
|---|---|
| 1 | Top-up (paid by your app), redeem, balance |
| 2 | Top-up where Facilipay charges the customer's card (`PAYMENT_COLLECTED = false`) and the `PAYMENT_` extras |

A new version adds extras and codes; it never changes what earlier ones mean. Ignore extras you
don't recognise.

## The protocol file

Every name and value on this page, in one machine-readable file:
[`protocol/gift-intent-protocol.properties`](https://github.com/Facilipay/facilipay-terminal-android/blob/main/protocol/gift-intent-protocol.properties).
The SDK and Facilipay's app are both tested against it, so it is always what Facilipay sends and
accepts. If your app builds intents itself, you can generate your constants from it.

```properties
--8<-- "protocol/gift-intent-protocol.properties"
```
