package io.facilipay.terminal

import android.app.Activity
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** The library's Android glue on a real device: availability, intents, and reading results. */
@RunWith(AndroidJUnit4::class)
class FacilipayGiftDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun facilipayInstalledFirst_isReady() {
        // Only meaningful on a Facilipay terminal where Facilipay was installed before this test app.
        val facilipayInstalled = runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(FacilipayGift.FACILIPAY_PACKAGE, 0)
        }.isSuccess
        assumeTrue("Facilipay is not installed on this device", facilipayInstalled)
        assertEquals(Availability.READY, FacilipayGift.availability(context))
    }

    @Test
    fun missingPackage_isNotInstalled() {
        assertEquals(Availability.NOT_INSTALLED, FacilipayGift.availability(context, "io.facilipay.not.installed"))
    }

    @Test
    fun installedAppWithoutTheEntryPoint_isNotSupported() {
        assertEquals(Availability.NOT_SUPPORTED, FacilipayGift.availability(context, "com.android.settings"))
    }

    @Test
    fun createIntent_carriesEveryExtraWithItsType() {
        val request = GiftRequest.topUp("order-1", 2_500L, GiftRequest.TopUpPayment.CHARGE_CARD, "eur")
        val intent = FacilipayGift.createIntent(request)

        assertEquals("com.facilipay.pos.action.GIFT_TRANSACTION", intent.action)
        assertEquals(FacilipayGift.FACILIPAY_PACKAGE, intent.`package`)
        assertEquals(true, intent.hasCategory(Intent.CATEGORY_DEFAULT))
        assertEquals("order-1", intent.getStringExtra("com.facilipay.pos.extra.REQUEST_ID"))
        assertEquals("TOPUP", intent.getStringExtra("com.facilipay.pos.extra.OPERATION"))
        assertEquals(2_500L, intent.getLongExtra("com.facilipay.pos.extra.AMOUNT_MINOR", -1L))
        assertEquals(false, intent.getBooleanExtra("com.facilipay.pos.extra.PAYMENT_COLLECTED", true))
        assertEquals(2, intent.getIntExtra("com.facilipay.pos.extra.API_VERSION", -1))
        assertEquals("EUR", intent.getStringExtra("com.facilipay.pos.extra.CURRENCY"))
    }

    @Test
    fun contract_readsARealResultIntent() {
        val contract = FacilipayGift.Contract()
        contract.createIntent(context, GiftRequest.balance("balance-1"))
        val data = Intent()
            .putExtra("com.facilipay.pos.result.STATUS", "APPROVED")
            .putExtra("com.facilipay.pos.result.REQUEST_ID", "balance-1")
            .putExtra("com.facilipay.pos.result.RESPONSE_CODE", "00")
            .putExtra("com.facilipay.pos.result.AMOUNT_MINOR", 0L)
            .putExtra("com.facilipay.pos.result.BALANCE_MINOR", 7_500L)

        val approved = assertIs<GiftResult.Approved>(contract.parseResult(Activity.RESULT_OK, data))
        assertEquals(7_500L, approved.giftCard.balanceMinor)

        // No data after the request was running: unknown, for the request that was sent.
        val unknown = assertIs<GiftResult.Unknown>(contract.parseResult(Activity.RESULT_CANCELED, null))
        assertEquals("balance-1", unknown.requestId)
    }
}
