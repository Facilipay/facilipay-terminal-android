package io.facilipay.terminal.sample

import android.content.Context
import android.util.Log
import io.facilipay.terminal.GiftResult
import io.facilipay.terminal.NextStep
import java.util.Locale

/** Turns a [GiftResult] into the text the sample shows, and logs it under the `FacilipaySample` tag. */
object ResultText {

    private const val TAG = "FacilipaySample"

    fun describe(context: Context, result: GiftResult): String {
        // --8<-- [start:handle-result]
        fun money(minor: Long) = String.format(Locale.ROOT, "%.2f", minor / 100.0)

        val outcome = when (result) {
            is GiftResult.Approved ->
                "Approved: ${money(result.giftCard.amountMinor)}, " +
                    "gift card balance ${money(result.giftCard.balanceMinor)}" +
                    if (result.isPartial) " (partial: the card held less than asked)" else ""
            is GiftResult.Cancelled -> "Cancelled on the terminal (${result.reason})"
            is GiftResult.Declined -> "Card declined. Nothing was charged."
            is GiftResult.Failed -> "Failed: ${result.reason} (${result.message})"
            is GiftResult.PaidNotCredited -> "Card charged, but the gift card was not loaded"
            is GiftResult.Unknown -> "Outcome not known yet"
        }
        // Whenever a card payment exists, show its reference: the merchant needs it to check or
        // refund the payment.
        val payment = result.cardPayment?.let {
            val charged = it.amountMinor?.let { minor -> ", charged ${money(minor)}" }.orEmpty()
            "Card payment ${it.status}$charged, reference ${it.reference}"
        }
        // --8<-- [end:handle-result]
        val text = listOfNotNull(
            outcome,
            payment,
            "",
            context.getString(nextStepText(result.nextStep)),
            "",
            "Request id: ${result.requestId}",
            "Response code: ${result.responseCode}",
        ).joinToString("\n")
        Log.i(TAG, result.toString())
        return text
    }

    private fun nextStepText(step: NextStep): Int = when (step) {
        NextStep.DONE -> R.string.next_done
        NextStep.START_NEW_REQUEST -> R.string.next_start_new
        NextStep.RESEND_SAME_REQUEST_ID -> R.string.next_resend
        NextStep.FIX_THEN_RETRY -> R.string.next_fix
        NextStep.REFUND_CARD_PAYMENT -> R.string.next_refund
        NextStep.CHECK_PAYMENT_WITH_MERCHANT -> R.string.next_check_payment
    }

}
