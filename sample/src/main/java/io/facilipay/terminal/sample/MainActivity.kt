package io.facilipay.terminal.sample

import android.content.ActivityNotFoundException
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.facilipay.terminal.Availability
import io.facilipay.terminal.FacilipayGift
import io.facilipay.terminal.GiftRequest
import io.facilipay.terminal.GiftResult
import io.facilipay.terminal.NextStep
import io.facilipay.terminal.sample.databinding.ActivityMainBinding
import java.util.UUID

/**
 * One screen that runs every gift operation through the Facilipay Terminal SDK and shows the
 * result. Pending requests are kept by [PendingRequests], so a request whose outcome isn't final
 * can be sent again with the same id, even after the app restarts.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var pendingRequests: PendingRequests

    // --8<-- [start:register]
    private val giftLauncher = registerForActivityResult(FacilipayGift.Contract()) { result ->
        showResult(result) // Your own UI, for example the handler in "Handle results".
        if (result.nextStep == NextStep.RESEND_SAME_REQUEST_ID ||
            result.nextStep == NextStep.CHECK_PAYMENT_WITH_MERCHANT
        ) {
            // Not final yet: keep the saved request, so the same id can be sent again.
        } else {
            pendingRequests.clear() // Final: forget the saved request.
        }
    }
    // --8<-- [end:register]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pendingRequests = PendingRequests(this)

        binding.topUpPaid.setOnClickListener {
            // --8<-- [start:top-up-paid]
            val request = GiftRequest.topUp(
                requestId = UUID.randomUUID().toString(), // Or your own order reference.
                amountMinor = amountMinor(), // Your amount in minor units: 2_500 is €25.00.
                payment = GiftRequest.TopUpPayment.ALREADY_COLLECTED,
            )
            // --8<-- [end:top-up-paid]
            send(request)
        }
        binding.topUpCharge.setOnClickListener {
            // --8<-- [start:top-up-charge]
            val request = GiftRequest.topUp(
                requestId = UUID.randomUUID().toString(),
                amountMinor = amountMinor(), // Charged to the customer's card on the terminal.
                payment = GiftRequest.TopUpPayment.CHARGE_CARD,
            )
            // --8<-- [end:top-up-charge]
            send(request)
        }
        binding.redeem.setOnClickListener {
            // --8<-- [start:redeem]
            val request = GiftRequest.redeem(
                requestId = UUID.randomUUID().toString(),
                amountMinor = amountMinor(),
                allowPartial = false, // true: take what the card holds when it holds less.
            )
            // --8<-- [end:redeem]
            send(
                if (binding.allowPartial.isChecked) {
                    GiftRequest.redeem(request.requestId, request.amountMinor!!, allowPartial = true)
                } else {
                    request
                },
            )
        }
        binding.balance.setOnClickListener {
            // --8<-- [start:balance]
            val request = GiftRequest.balance(requestId = UUID.randomUUID().toString())
            // --8<-- [end:balance]
            send(request)
        }
        binding.resend.setOnClickListener {
            pendingRequests.load()?.let(::send) ?: showMessage(getString(R.string.nothing_to_resend))
        }
    }

    override fun onResume() {
        super.onResume()
        showAvailability()
        binding.resend.isEnabled = pendingRequests.load() != null
    }

    private fun showAvailability() {
        // --8<-- [start:availability]
        // R.string.availability_*: your own messages for the merchant.
        val message = when (FacilipayGift.availability(this)) {
            Availability.READY -> R.string.availability_ready
            Availability.NOT_INSTALLED -> R.string.availability_not_installed
            Availability.NOT_SUPPORTED -> R.string.availability_not_supported
            Availability.PERMISSION_MISSING -> R.string.availability_permission_missing
        }
        // --8<-- [end:availability]
        binding.availability.setText(message)
    }

    private fun send(request: GiftRequest) {
        binding.resend.isEnabled = true
        // --8<-- [start:launch]
        // pendingRequests: your own store, such as the sample's PendingRequests or your order database.
        pendingRequests.save(request) // Save it before launching; see "Save the request".
        try {
            giftLauncher.launch(request)
        } catch (e: ActivityNotFoundException) {
            // Facilipay is not installed, or too old: FacilipayGift.availability() says which.
            pendingRequests.clear() // Nothing reached Facilipay.
            showAvailability()
        } catch (e: SecurityException) {
            // This app was installed before Facilipay: reinstall or update it to get the permission.
            pendingRequests.clear() // Nothing reached Facilipay.
            showAvailability()
        }
        // --8<-- [end:launch]
    }

    private fun showResult(result: GiftResult) {
        binding.result.text = ResultText.describe(this, result)
        binding.resend.isEnabled = pendingRequests.load() != null
    }

    private fun showMessage(message: String) {
        binding.result.text = message
    }

    private fun amountMinor(): Long {
        val euros = binding.amount.text.toString().toBigDecimalOrNull() ?: 5.toBigDecimal()
        return euros.movePointRight(2).toLong().coerceAtLeast(1L)
    }

}
