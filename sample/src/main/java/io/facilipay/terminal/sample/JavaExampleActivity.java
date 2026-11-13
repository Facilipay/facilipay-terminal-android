package io.facilipay.terminal.sample;

import android.content.ActivityNotFoundException;
import android.os.Bundle;

import java.util.UUID;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import io.facilipay.terminal.Availability;
import io.facilipay.terminal.FacilipayGift;
import io.facilipay.terminal.GiftRequest;
import io.facilipay.terminal.GiftResult;

/**
 * The same calls as {@link MainActivity}, in Java. It is not part of the sample's screen; it is
 * compiled with the sample so the Java code in the guide is known to build.
 */
public class JavaExampleActivity extends AppCompatActivity {

    // --8<-- [start:register]
    private final ActivityResultLauncher<GiftRequest> giftLauncher =
            registerForActivityResult(new FacilipayGift.Contract(), result -> {
                if (result instanceof GiftResult.Approved) {
                    GiftResult.Approved approved = (GiftResult.Approved) result;
                    long balanceMinor = approved.getGiftCard().getBalanceMinor();
                    // Show the new balance.
                }
                switch (result.getNextStep()) {
                    case RESEND_SAME_REQUEST_ID:
                    case CHECK_PAYMENT_WITH_MERCHANT:
                        // Not final yet: keep the request so the same id can be sent again.
                        break;
                    default:
                        // Final: forget the request.
                        break;
                }
            });
    // --8<-- [end:register]

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // --8<-- [start:availability]
        Availability availability = FacilipayGift.availability(this);
        if (availability != Availability.READY) {
            // Hide the gift option and tell the merchant why (see Availability).
        }
        // --8<-- [end:availability]
    }

    void topUpPaid(String orderId, long amountMinor) {
        // --8<-- [start:top-up-paid]
        // orderId: your own order reference. amountMinor: 2500 is €25.00.
        GiftRequest request = GiftRequest.topUp(
                orderId + "-load", amountMinor, GiftRequest.TopUpPayment.ALREADY_COLLECTED);
        // --8<-- [end:top-up-paid]
        launch(request);
    }

    void topUpCharge(String orderId, long amountMinor) {
        // --8<-- [start:top-up-charge]
        // orderId: your own order reference. amountMinor: 2500 is €25.00.
        GiftRequest request = GiftRequest.topUp(
                orderId + "-load", amountMinor, GiftRequest.TopUpPayment.CHARGE_CARD);
        // --8<-- [end:top-up-charge]
        launch(request);
    }

    void redeem(String orderId, long amountMinor) {
        // --8<-- [start:redeem]
        // orderId: your own order reference. amountMinor: 1000 is €10.00.
        GiftRequest request = GiftRequest.redeem(orderId + "-redeem", amountMinor, /* allowPartial */ false);
        // --8<-- [end:redeem]
        launch(request);
    }

    void balance() {
        // --8<-- [start:balance]
        // A new id for every balance check: re-using one returns the first check's result.
        GiftRequest request = GiftRequest.balance(UUID.randomUUID().toString());
        // --8<-- [end:balance]
        launch(request);
    }

    void launch(GiftRequest request) {
        // --8<-- [start:launch]
        try {
            giftLauncher.launch(request);
        } catch (ActivityNotFoundException | SecurityException e) {
            // Facilipay is missing, too old, or this app lacks its permission:
            // FacilipayGift.availability() says which.
        }
        // --8<-- [end:launch]
    }
}
