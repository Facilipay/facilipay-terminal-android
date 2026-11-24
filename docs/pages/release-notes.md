# Release notes

## 1.0.0

The first release of the Facilipay Terminal SDK (`io.facilipay:terminal-sdk`).

- Gift card top-up, redeem and balance check, through `FacilipayGift.Contract` and `GiftRequest`.
- Top-up where Facilipay charges the customer's card on the terminal
  (`TopUpPayment.CHARGE_CARD`), with the card payment's details in `GiftResult.cardPayment`.
- Typed results (`GiftResult`) with a `nextStep` for every outcome.
- `FacilipayGift.availability()` to check that your app can reach Facilipay.
- Needs a Facilipay version with app integration; `availability()` returns `NOT_SUPPORTED` on older
  ones.
