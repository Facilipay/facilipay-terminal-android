package io.facilipay.terminal.sample

import android.content.Context
import io.facilipay.terminal.GiftRequest

/**
 * Keeps the last request whose outcome isn't final, so it can be sent again with the **same**
 * request id, even after the app was closed. Sending the same id never runs the operation twice:
 * Facilipay returns the first request's result.
 *
 * It holds one request, because the sample sends one at a time: a result always belongs to the
 * saved request. A real app keeps the request with its order, in its own database.
 */
class PendingRequests(context: Context) {

    private val prefs = context.getSharedPreferences("pending_gift_request", Context.MODE_PRIVATE)

    fun save(request: GiftRequest) {
        prefs.edit()
            .putString(KEY_OPERATION, request.operation.name)
            .putString(KEY_REQUEST_ID, request.requestId)
            .putLong(KEY_AMOUNT, request.amountMinor ?: 0L)
            .putString(KEY_PAYMENT, request.topUpPayment?.name)
            .putBoolean(KEY_PARTIAL, request.allowPartial)
            .putString(KEY_CURRENCY, request.currency)
            // commit(), not apply(): the request must be on disk before Facilipay starts.
            .commit()
    }

    fun load(): GiftRequest? {
        val id = prefs.getString(KEY_REQUEST_ID, null) ?: return null
        val amount = prefs.getLong(KEY_AMOUNT, 0L)
        val currency = prefs.getString(KEY_CURRENCY, null)
        return when (prefs.getString(KEY_OPERATION, null)) {
            GiftRequest.Operation.TOP_UP.name -> GiftRequest.topUp(
                id, amount, GiftRequest.TopUpPayment.valueOf(prefs.getString(KEY_PAYMENT, null)!!), currency,
            )
            GiftRequest.Operation.REDEEM.name ->
                GiftRequest.redeem(id, amount, prefs.getBoolean(KEY_PARTIAL, false), currency)
            GiftRequest.Operation.BALANCE.name -> GiftRequest.balance(id, currency)
            else -> null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_OPERATION = "operation"
        const val KEY_REQUEST_ID = "request_id"
        const val KEY_AMOUNT = "amount_minor"
        const val KEY_PAYMENT = "top_up_payment"
        const val KEY_PARTIAL = "allow_partial"
        const val KEY_CURRENCY = "currency"
    }
}
