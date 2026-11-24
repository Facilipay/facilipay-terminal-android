package io.facilipay.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class GiftResultParsingTest {

    private class MapExtras(private val values: Map<String, Any>) : ResultExtras {
        override fun string(name: String): String? = values[name] as? String
        override fun long(name: String): Long? = values[name] as? Long
    }

    private val result = "com.facilipay.pos.result."

    private fun parse(vararg values: Pair<String, Any>, fallback: String? = "sent-id"): GiftResult =
        parseGiftResult(MapExtras(values.associate { (k, v) -> "$result$k" to v }), fallback)

    // The values of a real answer from the A920Pro, 2026-11-13.
    @Test
    fun approvedCardTopUp_carriesTheGiftCardAndThePayment() {
        val parsed = parse(
            "STATUS" to "APPROVED", "REQUEST_ID" to "rel-v2-charge-2", "RESPONSE_CODE" to "00",
            "MESSAGE" to "SUCCESSFUL", "SEQUENCE_NO" to "TPA23DFE1478D493",
            "TRANSACTION_UID" to "10800074303e", "AMOUNT_MINOR" to 500L, "BALANCE_MINOR" to 7_800L,
            "AUTH_CODE" to "164236", "MASKED_PAN" to "****9874", "RECEIPT_TEXT" to "receipt",
            "PAYMENT_STATUS" to "APPROVED", "PAYMENT_RESPONSE_CODE" to "00",
            "PAYMENT_REFERENCE" to "TC_7C3DBA60131F4", "PAYMENT_AUTH_CODE" to "366403",
            "PAYMENT_MASKED_PAN" to "418745******7458", "PAYMENT_AMOUNT_MINOR" to 500L,
            "PAYMENT_RECEIPT_TEXT" to "card receipt",
        )
        val approved = assertIs<GiftResult.Approved>(parsed)
        assertEquals("rel-v2-charge-2", approved.requestId)
        assertEquals(500L, approved.giftCard.amountMinor)
        assertEquals(7_800L, approved.giftCard.balanceMinor)
        assertEquals("****9874", approved.giftCard.maskedCardNumber)
        assertEquals("10800074303e", approved.giftCard.transactionId)
        assertEquals(false, approved.isPartial)
        assertEquals(PaymentStatus.APPROVED, approved.cardPayment?.status)
        assertEquals("TC_7C3DBA60131F4", approved.cardPayment?.reference)
        assertEquals(500L, approved.cardPayment?.amountMinor)
        assertEquals(NextStep.DONE, approved.nextStep)
    }

    @Test
    fun approvedWithoutPayment_hasNoCardPayment() {
        val approved = assertIs<GiftResult.Approved>(
            parse("STATUS" to "APPROVED", "RESPONSE_CODE" to "00", "AMOUNT_MINOR" to 0L, "BALANCE_MINOR" to 7_200L),
        )
        assertNull(approved.cardPayment)
    }

    @Test
    fun partialRedeem_isApprovedAndPartial() {
        val approved = assertIs<GiftResult.Approved>(
            parse("STATUS" to "APPROVED", "RESPONSE_CODE" to "10", "AMOUNT_MINOR" to 120L, "BALANCE_MINOR" to 0L),
        )
        assertEquals(true, approved.isPartial)
        assertEquals(120L, approved.giftCard.amountMinor)
    }

    @Test
    fun cancelledCodes_mapToReasons() {
        mapOf(
            "P3" to CancelReason.ON_TERMINAL,
            "P4" to CancelReason.BY_ANOTHER_REQUEST,
            "P5" to CancelReason.TIMED_OUT,
            "XX" to CancelReason.OTHER,
        ).forEach { (code, reason) ->
            val cancelled = assertIs<GiftResult.Cancelled>(parse("STATUS" to "CANCELLED", "RESPONSE_CODE" to code))
            assertEquals(reason, cancelled.reason, code)
            assertEquals(NextStep.START_NEW_REQUEST, cancelled.nextStep)
        }
    }

    @Test
    fun errorCodes_mapToReasonsAndNextSteps() {
        mapOf(
            "30" to (FailureReason.INVALID_REQUEST to NextStep.FIX_THEN_RETRY),
            "12" to (FailureReason.INVALID_REQUEST to NextStep.FIX_THEN_RETRY),
            "13" to (FailureReason.INVALID_REQUEST to NextStep.FIX_THEN_RETRY),
            "58" to (FailureReason.NOT_ENABLED to NextStep.FIX_THEN_RETRY),
            "27" to (FailureReason.NOT_LOGGED_IN to NextStep.FIX_THEN_RETRY),
            "09" to (FailureReason.BUSY to NextStep.RESEND_SAME_REQUEST_ID),
            "94" to (FailureReason.DUPLICATE_REQUEST_ID to NextStep.FIX_THEN_RETRY),
            "21" to (FailureReason.NOT_COMPLETED to NextStep.START_NEW_REQUEST),
            "51" to (FailureReason.INSUFFICIENT_BALANCE to NextStep.START_NEW_REQUEST),
            "54" to (FailureReason.CARD_EXPIRED to NextStep.START_NEW_REQUEST),
            "56" to (FailureReason.CARD_NOT_FOUND to NextStep.START_NEW_REQUEST),
            "14" to (FailureReason.CARD_NOT_LINKED to NextStep.START_NEW_REQUEST),
            "06" to (FailureReason.OTHER to NextStep.START_NEW_REQUEST),
        ).forEach { (code, expected) ->
            val failed = assertIs<GiftResult.Failed>(parse("STATUS" to "ERROR", "RESPONSE_CODE" to code), code)
            assertEquals(expected.first, failed.reason, code)
            assertEquals(expected.second, failed.nextStep, code)
        }
    }

    // A v1 decline from the gift flow (no card payment) stays a failure, not a card decline.
    @Test
    fun errorWithoutPayment_isFailedNotDeclined() {
        assertIs<GiftResult.Failed>(parse("STATUS" to "ERROR", "RESPONSE_CODE" to "05"))
    }

    @Test
    fun declinedCardPayment_isDeclined() {
        val declined = assertIs<GiftResult.Declined>(
            parse(
                "STATUS" to "ERROR", "RESPONSE_CODE" to "05", "MESSAGE" to "DECLINED",
                "PAYMENT_STATUS" to "DECLINED", "PAYMENT_RESPONSE_CODE" to "05",
                "PAYMENT_REFERENCE" to "TC_8EB2CB5CF98B4",
            ),
        )
        assertEquals("TC_8EB2CB5CF98B4", declined.cardPayment.reference)
        assertEquals(NextStep.START_NEW_REQUEST, declined.nextStep)
    }

    @Test
    fun paidNotCredited_asksForARefund() {
        val pc = assertIs<GiftResult.PaidNotCredited>(
            parse(
                "STATUS" to "ERROR", "RESPONSE_CODE" to "PC",
                "PAYMENT_STATUS" to "APPROVED", "PAYMENT_REFERENCE" to "TC_41CC405FC5984",
            ),
        )
        assertEquals("TC_41CC405FC5984", pc.cardPayment.reference)
        assertEquals(NextStep.REFUND_CARD_PAYMENT, pc.nextStep)
    }

    @Test
    fun paidNotCreditedWithoutPayment_stillSaysTheCardWasCharged() {
        val pc = assertIs<GiftResult.PaidNotCredited>(parse("STATUS" to "ERROR", "RESPONSE_CODE" to "PC"))
        assertEquals(PaymentStatus.APPROVED, pc.cardPayment.status)
    }

    @Test
    fun unknownAfterTheCardWasCharged_resendsTheSameId() {
        val unknown = assertIs<GiftResult.Unknown>(
            parse("STATUS" to "UNKNOWN", "RESPONSE_CODE" to "22", "PAYMENT_STATUS" to "APPROVED"),
        )
        assertEquals(NextStep.RESEND_SAME_REQUEST_ID, unknown.nextStep)
    }

    @Test
    fun unknownCardPayment_isCheckedWithTheMerchant() {
        val unknown = assertIs<GiftResult.Unknown>(
            parse("STATUS" to "UNKNOWN", "RESPONSE_CODE" to "22", "PAYMENT_STATUS" to "UNKNOWN"),
        )
        assertEquals(NextStep.CHECK_PAYMENT_WITH_MERCHANT, unknown.nextStep)
    }

    @Test
    fun unknownWithoutPayment_resendsTheSameId() {
        val unknown = assertIs<GiftResult.Unknown>(parse("STATUS" to "UNKNOWN", "RESPONSE_CODE" to "22"))
        assertEquals(NextStep.RESEND_SAME_REQUEST_ID, unknown.nextStep)
    }

    @Test
    fun noData_isUnknownForTheRequestThatWasSent() {
        val unknown = assertIs<GiftResult.Unknown>(parseGiftResult(null, "sent-id"))
        assertEquals("sent-id", unknown.requestId)
        assertEquals(NextStep.RESEND_SAME_REQUEST_ID, unknown.nextStep)
        assertIs<GiftResult.Unknown>(parseGiftResult(MapExtras(emptyMap()), null))
    }

    @Test
    fun unexpectedValues_areTreatedAsUnknown() {
        assertIs<GiftResult.Unknown>(parse("STATUS" to "SOMETHING_NEW", "RESPONSE_CODE" to "00"))
        val unknown = assertIs<GiftResult.Unknown>(
            parse("STATUS" to "UNKNOWN", "RESPONSE_CODE" to "22", "PAYMENT_STATUS" to "SOMETHING_NEW"),
        )
        assertEquals(PaymentStatus.UNKNOWN, unknown.cardPayment?.status)
        assertEquals(NextStep.CHECK_PAYMENT_WITH_MERCHANT, unknown.nextStep)
    }

    @Test
    fun requestId_fallsBackToTheSentOneOnlyWhenMissing() {
        assertEquals("echoed", parse("STATUS" to "ERROR", "RESPONSE_CODE" to "58", "REQUEST_ID" to "echoed").requestId)
        assertEquals("sent-id", parse("STATUS" to "ERROR", "RESPONSE_CODE" to "30").requestId)
    }
}
