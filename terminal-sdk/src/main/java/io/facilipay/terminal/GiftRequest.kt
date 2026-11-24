package io.facilipay.terminal

/**
 * A gift card operation to run in Facilipay. Create one with [topUp], [redeem] or [balance] and
 * launch it with [FacilipayGift.Contract].
 *
 * Every request carries your **request id**: a key you choose, unique per operation (for example
 * `"order-4711-load"`). Facilipay uses it to make the request safe to repeat. Sending the same id
 * again never runs the operation twice: you get the first request's result back. Keep the id until
 * you have a final result.
 *
 * The factories check their arguments and throw [IllegalArgumentException] for a request
 * Facilipay would refuse, so mistakes show up while you develop rather than on the terminal.
 */
public class GiftRequest private constructor(
    /** What to do. */
    public val operation: Operation,
    /** Your idempotency key, without leading or trailing spaces. */
    public val requestId: String,
    /** The amount in minor units (cents), or `null` for a balance check. */
    public val amountMinor: Long?,
    /** For a top-up: who takes the customer's money. `null` for the other operations. */
    public val topUpPayment: TopUpPayment?,
    /** For a redeem: whether to take what the card holds when it holds less than asked. */
    public val allowPartial: Boolean,
    /** The ISO 4217 currency code you expect, or `null` to use the merchant's. */
    public val currency: String?,
) {

    /** The operations Facilipay runs for your app. */
    public enum class Operation {
        /** Adds money to a gift card. */
        TOP_UP,

        /** Takes money off a gift card. */
        REDEEM,

        /** Reads a gift card's balance. Nothing is posted. */
        BALANCE,
    }

    /** Who takes the customer's money for a top-up. */
    public enum class TopUpPayment {
        /** Your app has already taken the money. Facilipay only loads the gift card. */
        ALREADY_COLLECTED,

        /**
         * Facilipay charges the customer's payment card on the terminal for the same amount, then
         * loads the gift card. The result carries both outcomes in [GiftResult.cardPayment].
         */
        CHARGE_CARD,
    }

    override fun equals(other: Any?): Boolean = other is GiftRequest &&
        operation == other.operation && requestId == other.requestId &&
        amountMinor == other.amountMinor && topUpPayment == other.topUpPayment &&
        allowPartial == other.allowPartial && currency == other.currency

    override fun hashCode(): Int =
        listOf(operation, requestId, amountMinor, topUpPayment, allowPartial, currency).hashCode()

    override fun toString(): String = "GiftRequest(operation=$operation, requestId=$requestId, " +
        "amountMinor=$amountMinor, topUpPayment=$topUpPayment, allowPartial=$allowPartial, " +
        "currency=$currency)"

    public companion object {

        /**
         * Loads [amountMinor] onto the gift card the merchant presents.
         *
         * @param requestId your idempotency key, 1 to 128 characters.
         * @param amountMinor the amount in minor units, 1 to 99,999,999 (for example `2500` for €25.00).
         * @param payment who takes the customer's money: your app already did, or Facilipay charges
         *   the customer's card on the terminal.
         * @param currency optional ISO 4217 code; Facilipay refuses the request if it is not the
         *   merchant's currency.
         */
        @JvmStatic
        @JvmOverloads
        public fun topUp(
            requestId: String,
            amountMinor: Long,
            payment: TopUpPayment,
            currency: String? = null,
        ): GiftRequest = GiftRequest(
            operation = Operation.TOP_UP,
            requestId = checkRequestId(requestId),
            amountMinor = checkAmount(amountMinor),
            topUpPayment = payment,
            allowPartial = false,
            currency = checkCurrency(currency),
        )

        /**
         * Takes [amountMinor] off the gift card the merchant presents.
         *
         * @param requestId your idempotency key, 1 to 128 characters.
         * @param amountMinor the amount in minor units, 1 to 99,999,999.
         * @param allowPartial when the card holds less than [amountMinor], take what it holds
         *   instead of declining. The result's [GiftCard.amountMinor] says what was taken.
         * @param currency optional ISO 4217 code; Facilipay refuses the request if it is not the
         *   merchant's currency.
         */
        @JvmStatic
        @JvmOverloads
        public fun redeem(
            requestId: String,
            amountMinor: Long,
            allowPartial: Boolean = false,
            currency: String? = null,
        ): GiftRequest = GiftRequest(
            operation = Operation.REDEEM,
            requestId = checkRequestId(requestId),
            amountMinor = checkAmount(amountMinor),
            topUpPayment = null,
            allowPartial = allowPartial,
            currency = checkCurrency(currency),
        )

        /**
         * Reads the balance of the gift card the merchant presents. Nothing is posted.
         *
         * @param requestId your idempotency key, 1 to 128 characters.
         * @param currency optional ISO 4217 code; Facilipay refuses the request if it is not the
         *   merchant's currency.
         */
        @JvmStatic
        @JvmOverloads
        public fun balance(requestId: String, currency: String? = null): GiftRequest = GiftRequest(
            operation = Operation.BALANCE,
            requestId = checkRequestId(requestId),
            amountMinor = null,
            topUpPayment = null,
            allowPartial = false,
            currency = checkCurrency(currency),
        )

        private fun checkRequestId(requestId: String): String {
            val trimmed = requestId.trim()
            require(trimmed.length in 1..GiftProtocol.MAX_REQUEST_ID_LENGTH) {
                "requestId must be 1 to ${GiftProtocol.MAX_REQUEST_ID_LENGTH} characters"
            }
            return trimmed
        }

        private fun checkAmount(amountMinor: Long): Long {
            require(amountMinor in 1..GiftProtocol.MAX_AMOUNT_MINOR) {
                "amountMinor must be 1 to ${GiftProtocol.MAX_AMOUNT_MINOR} minor units"
            }
            return amountMinor
        }

        private fun checkCurrency(currency: String?): String? {
            if (currency == null) return null
            require(currency.length == 3 && currency.all { it in 'A'..'Z' || it in 'a'..'z' }) {
                "currency must be a three-letter ISO 4217 code, such as EUR"
            }
            return currency.uppercase()
        }
    }
}

/** The API version a request needs: 2 only when Facilipay charges the card. */
internal val GiftRequest.apiVersion: Int
    get() = if (topUpPayment == GiftRequest.TopUpPayment.CHARGE_CARD) {
        GiftProtocol.API_VERSION_2
    } else {
        GiftProtocol.API_VERSION_1
    }

/** The request's intent extras, by name. Kept free of Android types so it can be unit tested. */
internal fun GiftRequest.extras(): Map<String, Any> = buildMap {
    put(GiftProtocol.EXTRA_REQUEST_ID, requestId)
    put(
        GiftProtocol.EXTRA_OPERATION,
        when (operation) {
            GiftRequest.Operation.TOP_UP -> GiftProtocol.OPERATION_TOPUP
            GiftRequest.Operation.REDEEM -> GiftProtocol.OPERATION_REDEEM
            GiftRequest.Operation.BALANCE -> GiftProtocol.OPERATION_BALANCE
        },
    )
    put(GiftProtocol.EXTRA_API_VERSION, apiVersion)
    amountMinor?.let { put(GiftProtocol.EXTRA_AMOUNT_MINOR, it) }
    topUpPayment?.let {
        put(GiftProtocol.EXTRA_PAYMENT_COLLECTED, it == GiftRequest.TopUpPayment.ALREADY_COLLECTED)
    }
    if (operation == GiftRequest.Operation.REDEEM && allowPartial) {
        put(GiftProtocol.EXTRA_ALLOW_PARTIAL, true)
    }
    currency?.let { put(GiftProtocol.EXTRA_CURRENCY, it) }
}
