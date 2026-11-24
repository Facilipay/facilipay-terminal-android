package io.facilipay.terminal

/** Read access to a result's extras, kept free of Android types so parsing can be unit tested. */
internal interface ResultExtras {
    fun string(name: String): String?
    fun long(name: String): Long?
}

/**
 * Turns Facilipay's result extras into a [GiftResult]. [fallbackRequestId] is the id that was sent,
 * used when Facilipay returned no data. Anything unexpected is treated as [GiftResult.Unknown], so
 * an integrator is never told "nothing happened" when something may have.
 */
internal fun parseGiftResult(extras: ResultExtras?, fallbackRequestId: String?): GiftResult {
    val status = extras?.string(GiftProtocol.RESULT_STATUS)
        ?: return GiftResult.Unknown(fallbackRequestId, null, null, null, null)
    val requestId = extras.string(GiftProtocol.RESULT_REQUEST_ID) ?: fallbackRequestId
    val code = extras.string(GiftProtocol.RESULT_RESPONSE_CODE)
    val message = extras.string(GiftProtocol.RESULT_MESSAGE)
    val sequenceNo = extras.string(GiftProtocol.RESULT_SEQUENCE_NO)
    val payment = parseCardPayment(extras)

    return when (status) {
        GiftProtocol.STATUS_APPROVED -> GiftResult.Approved(
            requestId, code, message, sequenceNo, payment,
            giftCard = GiftCard(
                amountMinor = extras.long(GiftProtocol.RESULT_AMOUNT_MINOR) ?: 0L,
                balanceMinor = extras.long(GiftProtocol.RESULT_BALANCE_MINOR) ?: 0L,
                maskedCardNumber = extras.string(GiftProtocol.RESULT_MASKED_PAN),
                transactionId = extras.string(GiftProtocol.RESULT_TRANSACTION_UID),
                authCode = extras.string(GiftProtocol.RESULT_AUTH_CODE),
                receiptText = extras.string(GiftProtocol.RESULT_RECEIPT_TEXT),
            ),
            isPartial = code == GiftProtocol.CODE_PARTIAL,
        )

        GiftProtocol.STATUS_CANCELLED -> GiftResult.Cancelled(
            requestId, code, message, sequenceNo, payment,
            reason = when (code) {
                GiftProtocol.CODE_CANCELLED_ON_TERMINAL -> CancelReason.ON_TERMINAL
                GiftProtocol.CODE_CANCELLED_BY_ANOTHER_REQUEST -> CancelReason.BY_ANOTHER_REQUEST
                GiftProtocol.CODE_TIMED_OUT -> CancelReason.TIMED_OUT
                else -> CancelReason.OTHER
            },
        )

        GiftProtocol.STATUS_ERROR -> when {
            // Charged, not credited. Facilipay always sends the payment with it; the fallback keeps
            // the "card was charged" meaning even if it did not.
            code == GiftProtocol.CODE_PAID_NOT_CREDITED -> GiftResult.PaidNotCredited(
                requestId, code, message, sequenceNo,
                payment ?: CardPayment(PaymentStatus.APPROVED, null, null, null, null, null, null),
            )

            payment?.status == PaymentStatus.DECLINED ->
                GiftResult.Declined(requestId, code, message, sequenceNo, payment)

            else -> GiftResult.Failed(requestId, code, message, sequenceNo, payment, failureReason(code))
        }

        else -> GiftResult.Unknown(requestId, code, message, sequenceNo, payment)
    }
}

private fun parseCardPayment(extras: ResultExtras): CardPayment? {
    val status = extras.string(GiftProtocol.RESULT_PAYMENT_STATUS) ?: return null
    return CardPayment(
        status = when (status) {
            GiftProtocol.PAYMENT_STATUS_NOT_TAKEN -> PaymentStatus.NOT_TAKEN
            GiftProtocol.PAYMENT_STATUS_APPROVED -> PaymentStatus.APPROVED
            GiftProtocol.PAYMENT_STATUS_DECLINED -> PaymentStatus.DECLINED
            GiftProtocol.PAYMENT_STATUS_CANCELLED -> PaymentStatus.CANCELLED
            // Includes PAYMENT_STATUS_UNKNOWN and any value a newer Facilipay adds.
            else -> PaymentStatus.UNKNOWN
        },
        reference = extras.string(GiftProtocol.RESULT_PAYMENT_REFERENCE),
        responseCode = extras.string(GiftProtocol.RESULT_PAYMENT_RESPONSE_CODE),
        authCode = extras.string(GiftProtocol.RESULT_PAYMENT_AUTH_CODE),
        maskedCardNumber = extras.string(GiftProtocol.RESULT_PAYMENT_MASKED_PAN),
        amountMinor = extras.long(GiftProtocol.RESULT_PAYMENT_AMOUNT_MINOR),
        receiptText = extras.string(GiftProtocol.RESULT_PAYMENT_RECEIPT_TEXT),
    )
}

private fun failureReason(code: String?): FailureReason = when (code) {
    GiftProtocol.CODE_MALFORMED,
    GiftProtocol.CODE_UNKNOWN_OPERATION,
    GiftProtocol.CODE_INVALID_AMOUNT,
    -> FailureReason.INVALID_REQUEST
    GiftProtocol.CODE_NOT_ENABLED -> FailureReason.NOT_ENABLED
    GiftProtocol.CODE_NOT_LOGGED_IN -> FailureReason.NOT_LOGGED_IN
    GiftProtocol.CODE_IN_PROGRESS -> FailureReason.BUSY
    GiftProtocol.CODE_DUPLICATE -> FailureReason.DUPLICATE_REQUEST_ID
    GiftProtocol.CODE_NOT_STARTED -> FailureReason.NOT_COMPLETED
    GiftProtocol.CODE_INSUFFICIENT_BALANCE -> FailureReason.INSUFFICIENT_BALANCE
    GiftProtocol.CODE_CARD_EXPIRED -> FailureReason.CARD_EXPIRED
    GiftProtocol.CODE_CARD_NOT_FOUND -> FailureReason.CARD_NOT_FOUND
    GiftProtocol.CODE_CARD_NOT_LINKED -> FailureReason.CARD_NOT_LINKED
    else -> FailureReason.OTHER
}
