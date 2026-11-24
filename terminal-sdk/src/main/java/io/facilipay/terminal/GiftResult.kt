package io.facilipay.terminal

/**
 * What happened to a [GiftRequest]. Check the type, then follow [nextStep]:
 *
 * - [Approved]: done. [NextStep.DONE].
 * - [Cancelled]: nothing was posted or charged. [NextStep.START_NEW_REQUEST].
 * - [Declined]: the customer's card was declined; nothing was posted or charged.
 *   [NextStep.START_NEW_REQUEST].
 * - [Failed]: nothing was posted or charged; [Failed.reason] says why, and decides [nextStep].
 * - [PaidNotCredited]: the card was charged, the gift card was not loaded.
 *   [NextStep.REFUND_CARD_PAYMENT].
 * - [Unknown]: it may have been posted. [NextStep.RESEND_SAME_REQUEST_ID], or
 *   [NextStep.CHECK_PAYMENT_WITH_MERCHANT] when the card payment's outcome is unknown.
 *
 * `toString()` is safe to log: it holds no receipt text or authorisation codes, and card numbers
 * only in masked form.
 */
public sealed class GiftResult {

    /** Your request id, as Facilipay matched it. `null` only when Facilipay returned no data. */
    public abstract val requestId: String?

    /** Facilipay's two-character response code (`"00"` for approved), when it returned one. */
    public abstract val responseCode: String?

    /** A short description of [responseCode], at most 32 characters. */
    public abstract val message: String?

    /** Facilipay's reference for the request, when it created one. */
    public abstract val sequenceNo: String?

    /**
     * The customer's card payment, for a top-up Facilipay charged
     * ([GiftRequest.TopUpPayment.CHARGE_CARD]). `null` for every other request, and for a card
     * top-up Facilipay refused before running anything (for example [FailureReason.NOT_LOGGED_IN]).
     * Always set on [Declined] and [PaidNotCredited].
     */
    public abstract val cardPayment: CardPayment?

    /** What your app should do now. */
    public abstract val nextStep: NextStep

    /** The operation completed. */
    public class Approved internal constructor(
        override val requestId: String?,
        override val responseCode: String?,
        override val message: String?,
        override val sequenceNo: String?,
        override val cardPayment: CardPayment?,
        /** The gift card's side of the operation. */
        public val giftCard: GiftCard,
        /** A redeem took less than asked because the card held less ([GiftRequest.redeem] `allowPartial`). */
        public val isPartial: Boolean,
    ) : GiftResult() {
        override val nextStep: NextStep get() = NextStep.DONE
        override fun toString(): String = describe("Approved", "giftCard=$giftCard, isPartial=$isPartial")
    }

    /**
     * Cancelled on the terminal before anything was posted or charged. You may start again with a
     * new request id.
     */
    public class Cancelled internal constructor(
        override val requestId: String?,
        override val responseCode: String?,
        override val message: String?,
        override val sequenceNo: String?,
        override val cardPayment: CardPayment?,
        /** Why it was cancelled. */
        public val reason: CancelReason,
    ) : GiftResult() {
        override val nextStep: NextStep get() = NextStep.START_NEW_REQUEST
        override fun toString(): String = describe("Cancelled", "reason=$reason")
    }

    /**
     * The customer's card was declined for a top-up Facilipay charged. Nothing was charged and the
     * gift card was not loaded. You may start again with a new request id.
     */
    public class Declined internal constructor(
        override val requestId: String?,
        override val responseCode: String?,
        override val message: String?,
        override val sequenceNo: String?,
        override val cardPayment: CardPayment,
    ) : GiftResult() {
        override val nextStep: NextStep get() = NextStep.START_NEW_REQUEST
        override fun toString(): String = describe("Declined", "")
    }

    /** Nothing was posted or charged. [reason] says why, and [nextStep] what to do about it. */
    public class Failed internal constructor(
        override val requestId: String?,
        override val responseCode: String?,
        override val message: String?,
        override val sequenceNo: String?,
        override val cardPayment: CardPayment?,
        /** Why it failed. */
        public val reason: FailureReason,
    ) : GiftResult() {
        override val nextStep: NextStep get() = reason.nextStep
        override fun toString(): String = describe("Failed", "reason=$reason")
    }

    /**
     * The customer's card was charged, but the gift card was not loaded. The merchant refunds the
     * card payment on the terminal, using [CardPayment.reference]. Sending the same request id again
     * returns this result again.
     */
    public class PaidNotCredited internal constructor(
        override val requestId: String?,
        override val responseCode: String?,
        override val message: String?,
        override val sequenceNo: String?,
        override val cardPayment: CardPayment,
    ) : GiftResult() {
        override val nextStep: NextStep get() = NextStep.REFUND_CARD_PAYMENT
        override fun toString(): String = describe("PaidNotCredited", "")
    }

    /**
     * The request was interrupted and **may have been posted**. Never start a new request for the
     * same purchase yet: follow [nextStep].
     *
     * Also returned when Facilipay sent back no data at all, for example because it was closed
     * while the request was running.
     */
    public class Unknown internal constructor(
        override val requestId: String?,
        override val responseCode: String?,
        override val message: String?,
        override val sequenceNo: String?,
        override val cardPayment: CardPayment?,
    ) : GiftResult() {
        override val nextStep: NextStep
            get() = if (cardPayment?.status == PaymentStatus.UNKNOWN) {
                NextStep.CHECK_PAYMENT_WITH_MERCHANT
            } else {
                NextStep.RESEND_SAME_REQUEST_ID
            }

        override fun toString(): String = describe("Unknown", "")
    }

    internal fun describe(name: String, details: String): String = buildString {
        append("GiftResult.").append(name).append("(requestId=").append(requestId)
        append(", responseCode=").append(responseCode).append(", message=").append(message)
        if (details.isNotEmpty()) append(", ").append(details)
        if (cardPayment != null) append(", cardPayment=").append(cardPayment)
        append(", nextStep=").append(nextStep).append(')')
    }
}

/** What your app should do after a [GiftResult]. */
public enum class NextStep {
    /** Finished. Nothing more to do. */
    DONE,

    /** Nothing was posted or charged. You may start again, with a new request id. */
    START_NEW_REQUEST,

    /**
     * The outcome isn't final yet. Send the **same** request (same id) again later, for example
     * once your screen is back, to get the final result. Never use a new id for the same purchase.
     */
    RESEND_SAME_REQUEST_ID,

    /**
     * Facilipay or the request needs fixing first: see [Failed.reason][GiftResult.Failed.reason].
     * For example, the merchant must log in, or the integration must be switched on.
     */
    FIX_THEN_RETRY,

    /** The customer's card was charged and the gift card was not loaded: the merchant refunds it. */
    REFUND_CARD_PAYMENT,

    /**
     * The customer's card **may** have been charged. Don't take another payment for this purchase
     * until the merchant has checked [CardPayment.reference] with the payment provider.
     */
    CHECK_PAYMENT_WITH_MERCHANT,
}

/** Why a request was cancelled. */
public enum class CancelReason {
    /** The merchant cancelled or left the flow on the terminal (`P3`). */
    ON_TERMINAL,

    /** Another request on the terminal took over (`P4`). */
    BY_ANOTHER_REQUEST,

    /** The terminal timed out waiting for the merchant (`P5`). */
    TIMED_OUT,

    /** Any other cancellation code. */
    OTHER,
}

/** Why a request failed. Nothing was posted or charged. */
public enum class FailureReason(internal val nextStep: NextStep) {
    /**
     * The request was malformed or not accepted (`30`, `12`, `13`), for example a currency that is
     * not the merchant's. The library checks most of this before sending.
     */
    INVALID_REQUEST(NextStep.FIX_THEN_RETRY),

    /**
     * The integration is not switched on for this terminal (`58`), or this Facilipay build cannot
     * charge cards. The merchant asks Facilipay support to enable it.
     */
    NOT_ENABLED(NextStep.FIX_THEN_RETRY),

    /** Nobody is logged in to Facilipay (`27`). The merchant logs in, then you retry. */
    NOT_LOGGED_IN(NextStep.FIX_THEN_RETRY),

    /**
     * Something else is running on the terminal (`09`), possibly your own earlier request with the
     * same id. Wait until your screen is back, then send the same request again.
     */
    BUSY(NextStep.RESEND_SAME_REQUEST_ID),

    /**
     * The request id was already used for a different operation or amount (`94`). That earlier
     * request may have posted. Use a new id for a new purchase.
     */
    DUPLICATE_REQUEST_ID(NextStep.FIX_THEN_RETRY),

    /** The request never completed and nothing was posted (`21`). */
    NOT_COMPLETED(NextStep.START_NEW_REQUEST),

    /** The gift card holds less than the amount (`51`). */
    INSUFFICIENT_BALANCE(NextStep.START_NEW_REQUEST),

    /** The gift card has expired (`54`). */
    CARD_EXPIRED(NextStep.START_NEW_REQUEST),

    /** The gift card was not found (`56`). */
    CARD_NOT_FOUND(NextStep.START_NEW_REQUEST),

    /** The gift card is not linked to this merchant (`14`). */
    CARD_NOT_LINKED(NextStep.START_NEW_REQUEST),

    /** Any other code; see [GiftResult.responseCode] and [GiftResult.message]. */
    OTHER(NextStep.START_NEW_REQUEST),
}

/** The gift card's side of an approved operation. */
public class GiftCard internal constructor(
    /** The amount applied, in minor units: less than asked on a partial redeem, `0` for a balance check. */
    public val amountMinor: Long,
    /** The card's balance afterwards, in minor units. */
    public val balanceMinor: Long,
    /** The gift card, masked (for example `****9874`), when a card was read. */
    public val maskedCardNumber: String?,
    /** The transaction's reference in Facilipay. */
    public val transactionId: String?,
    /** An authorisation code Facilipay generates for the receipt. */
    public val authCode: String?,
    /** The receipt text, when Facilipay returned one. */
    public val receiptText: String?,
) {
    override fun toString(): String = "GiftCard(amountMinor=$amountMinor, balanceMinor=$balanceMinor, " +
        "maskedCardNumber=$maskedCardNumber, transactionId=$transactionId)"
}

/** The status of the customer's card payment for a top-up Facilipay charged. */
public enum class PaymentStatus {
    /** No card payment was taken. */
    NOT_TAKEN,

    /** The card was charged. */
    APPROVED,

    /** The card was declined. Nothing was charged. */
    DECLINED,

    /** The card payment was cancelled on the terminal. Nothing was charged. */
    CANCELLED,

    /** The card may have been charged. */
    UNKNOWN,
}

/** The customer's card payment, for a top-up Facilipay charged on the terminal. */
public class CardPayment internal constructor(
    /** Whether the card was charged. */
    public val status: PaymentStatus,
    /**
     * The payment's reference at the payment provider (`TC_` and 13 characters), once a card payment
     * started. The merchant uses it to find or refund the payment. Keep it with your order.
     */
    public val reference: String?,
    /** The card reader's two-character response code, once it answered. */
    public val responseCode: String?,
    /** The card issuer's authorisation code, when it returned one. */
    public val authCode: String?,
    /** The payment card, masked (for example `418745******7458`), when a card was read. */
    public val maskedCardNumber: String?,
    /** What the card was charged, in minor units, when approved. */
    public val amountMinor: Long?,
    /** The card payment's receipt text, when approved. */
    public val receiptText: String?,
) {
    override fun toString(): String = "CardPayment(status=$status, reference=$reference, " +
        "responseCode=$responseCode, maskedCardNumber=$maskedCardNumber, amountMinor=$amountMinor)"
}
