package io.facilipay.terminal

import io.facilipay.terminal.GiftRequest.TopUpPayment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GiftRequestTest {

    private val extra = "com.facilipay.pos.extra."

    @Test
    fun topUpAlreadyPaid_isVersion1WithPaymentCollected() {
        assertEquals(
            mapOf(
                "${extra}REQUEST_ID" to "order-1",
                "${extra}OPERATION" to "TOPUP",
                "${extra}API_VERSION" to 1,
                "${extra}AMOUNT_MINOR" to 2_500L,
                "${extra}PAYMENT_COLLECTED" to true,
            ),
            GiftRequest.topUp("order-1", 2_500L, TopUpPayment.ALREADY_COLLECTED).extras(),
        )
    }

    @Test
    fun topUpFacilipayCharges_isVersion2WithPaymentNotCollected() {
        assertEquals(
            mapOf(
                "${extra}REQUEST_ID" to "order-2",
                "${extra}OPERATION" to "TOPUP",
                "${extra}API_VERSION" to 2,
                "${extra}AMOUNT_MINOR" to 500L,
                "${extra}PAYMENT_COLLECTED" to false,
                "${extra}CURRENCY" to "EUR",
            ),
            GiftRequest.topUp("order-2", 500L, TopUpPayment.CHARGE_CARD, currency = "eur").extras(),
        )
    }

    @Test
    fun redeem_sendsAllowPartialOnlyWhenAsked() {
        assertEquals(
            mapOf(
                "${extra}REQUEST_ID" to "r-1",
                "${extra}OPERATION" to "REDEEM",
                "${extra}API_VERSION" to 1,
                "${extra}AMOUNT_MINOR" to 300L,
            ),
            GiftRequest.redeem("r-1", 300L).extras(),
        )
        assertEquals(true, GiftRequest.redeem("r-2", 300L, allowPartial = true).extras()["${extra}ALLOW_PARTIAL"])
    }

    @Test
    fun balance_hasNoAmount() {
        assertEquals(
            mapOf(
                "${extra}REQUEST_ID" to "b-1",
                "${extra}OPERATION" to "BALANCE",
                "${extra}API_VERSION" to 1,
            ),
            GiftRequest.balance("b-1").extras(),
        )
    }

    @Test
    fun requestId_isTrimmedAndLimited() {
        assertEquals("order-3", GiftRequest.balance("  order-3 ").requestId)
        assertEquals(128, GiftRequest.balance("x".repeat(128)).requestId.length)
        assertFailsWith<IllegalArgumentException> { GiftRequest.balance("   ") }
        assertFailsWith<IllegalArgumentException> { GiftRequest.balance("x".repeat(129)) }
    }

    @Test
    fun amount_mustBePositiveAndAtMost99999999() {
        GiftRequest.redeem("a", 99_999_999L)
        assertFailsWith<IllegalArgumentException> { GiftRequest.redeem("a", 0L) }
        assertFailsWith<IllegalArgumentException> { GiftRequest.redeem("a", -5L) }
        assertFailsWith<IllegalArgumentException> {
            GiftRequest.topUp("a", 100_000_000L, TopUpPayment.ALREADY_COLLECTED)
        }
    }

    @Test
    fun currency_mustBeThreeLetters() {
        assertFailsWith<IllegalArgumentException> { GiftRequest.balance("a", currency = "EURO") }
        assertFailsWith<IllegalArgumentException> { GiftRequest.balance("a", currency = "97") }
    }

    @Test
    fun equalRequests_areEqual() {
        assertEquals(
            GiftRequest.topUp("a", 1L, TopUpPayment.CHARGE_CARD),
            GiftRequest.topUp(" a", 1L, TopUpPayment.CHARGE_CARD),
        )
    }
}
