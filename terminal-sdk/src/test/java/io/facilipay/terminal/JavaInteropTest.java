package io.facilipay.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The calls a Java integrator makes, so the public API stays usable from Java. */
public class JavaInteropTest {

    @Test
    public void requestsAndResults_readNaturallyFromJava() {
        GiftRequest topUp = GiftRequest.topUp("order-1", 2500L, GiftRequest.TopUpPayment.CHARGE_CARD);
        GiftRequest redeem = GiftRequest.redeem("order-2", 300L);
        GiftRequest partial = GiftRequest.redeem("order-3", 300L, true);
        GiftRequest balance = GiftRequest.balance("balance-1");

        assertEquals(GiftRequest.Operation.TOP_UP, topUp.getOperation());
        assertEquals(Long.valueOf(300L), redeem.getAmountMinor());
        assertTrue(partial.getAllowPartial());
        assertEquals("balance-1", balance.getRequestId());
        assertEquals("com.facilipay.pax.pos", FacilipayGift.FACILIPAY_PACKAGE);
        assertEquals("com.facilipay.pax.pos.permission.GIFT_INTEGRATION", FacilipayGift.permission());

        FacilipayGift.Contract contract = new FacilipayGift.Contract();
        assertTrue(contract != null);

        GiftResult result = GiftResultParserKt.parseGiftResult(null, "order-1");
        if (result instanceof GiftResult.Unknown) {
            assertEquals(NextStep.RESEND_SAME_REQUEST_ID, result.getNextStep());
        } else {
            throw new AssertionError("expected Unknown");
        }
    }
}
