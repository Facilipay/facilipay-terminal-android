package io.facilipay.terminal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import androidx.activity.result.contract.ActivityResultContract

/**
 * Starts gift card operations in Facilipay from your app on the same terminal.
 *
 * ```kotlin
 * private val gift = registerForActivityResult(FacilipayGift.Contract()) { result ->
 *     when (result) {
 *         is GiftResult.Approved -> showBalance(result.giftCard.balanceMinor)
 *         else -> handle(result.nextStep)
 *     }
 * }
 *
 * // A new request id for every new operation: re-using one returns the first request's result.
 * gift.launch(GiftRequest.balance(requestId = UUID.randomUUID().toString()))
 * ```
 *
 * Facilipay comes to the front, the merchant presents the gift card, and your callback receives
 * the [GiftResult] when the merchant is done.
 *
 * [Contract] needs an AndroidX `ComponentActivity` (such as `AppCompatActivity`) or an AndroidX
 * `Fragment`. From a plain `android.app.Activity`, start [createIntent] with
 * `startActivityForResult` and read the result with [parseResult].
 */
public object FacilipayGift {

    /** The Facilipay app on Facilipay's PAX terminals. */
    public const val FACILIPAY_PACKAGE: String = "com.facilipay.pax.pos"

    /**
     * The permission your app needs to call [targetPackage]. The library declares it for
     * [FACILIPAY_PACKAGE]; add a `<uses-permission>` for this name only when you target another
     * Facilipay build.
     */
    @JvmStatic
    @JvmOverloads
    public fun permission(targetPackage: String = FACILIPAY_PACKAGE): String =
        targetPackage + GiftProtocol.PERMISSION_SUFFIX

    /**
     * Whether your app can call Facilipay on this device right now. Check it before showing a gift
     * option, and tell the merchant what to do when it isn't [Availability.READY].
     *
     * It cannot tell whether the merchant has the integration switched on; a request then returns
     * [GiftResult.Failed] with [FailureReason.NOT_ENABLED].
     */
    @JvmStatic
    @JvmOverloads
    public fun availability(context: Context, targetPackage: String = FACILIPAY_PACKAGE): Availability {
        val packageManager = context.packageManager
        try {
            @Suppress("DEPRECATION") // The flags overload is API 33+; this one works on all versions.
            packageManager.getPackageInfo(targetPackage, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            return Availability.NOT_INSTALLED
        }
        @Suppress("DEPRECATION")
        if (packageManager.resolveActivity(probeIntent(targetPackage), 0) == null) {
            return Availability.NOT_SUPPORTED
        }
        val granted = context.checkPermission(
            permission(targetPackage), Process.myPid(), Process.myUid(),
        ) == PackageManager.PERMISSION_GRANTED
        return if (granted) Availability.READY else Availability.PERMISSION_MISSING
    }

    /**
     * The intent for [request], for apps that use `startActivityForResult` instead of
     * [Contract]. Read the outcome with [parseResult]. Always start it **for a result**: started
     * with `startActivity`, or with `FLAG_ACTIVITY_NEW_TASK`, Facilipay cannot answer and refuses it.
     */
    @JvmStatic
    @JvmOverloads
    public fun createIntent(request: GiftRequest, targetPackage: String = FACILIPAY_PACKAGE): Intent =
        probeIntent(targetPackage).apply {
            for ((name, value) in request.extras()) {
                when (value) {
                    is String -> putExtra(name, value)
                    is Long -> putExtra(name, value)
                    is Int -> putExtra(name, value)
                    is Boolean -> putExtra(name, value)
                    else -> error("Unsupported extra type for $name")
                }
            }
        }

    /** The [GiftResult] in a result returned to `onActivityResult`. */
    @JvmStatic
    public fun parseResult(@Suppress("UNUSED_PARAMETER") resultCode: Int, data: Intent?): GiftResult =
        parseGiftResult(data?.let(::IntentResultExtras), fallbackRequestId = null)

    /**
     * The [ActivityResultContract] for `registerForActivityResult`. Launch it with a
     * [GiftRequest]; the callback receives the [GiftResult].
     *
     * Launch one request at a time, and wait for its result before launching the next.
     *
     * @param targetPackage the Facilipay app to call; only change it for a non-production build.
     */
    public class Contract @JvmOverloads constructor(
        private val targetPackage: String = FACILIPAY_PACKAGE,
    ) : ActivityResultContract<GiftRequest, GiftResult>() {

        // Lets a result with no data still name the request it answers, while this process lives.
        private var lastRequestId: String? = null

        override fun createIntent(context: Context, input: GiftRequest): Intent {
            lastRequestId = input.requestId
            return FacilipayGift.createIntent(input, targetPackage)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): GiftResult =
            parseGiftResult(intent?.let(::IntentResultExtras), fallbackRequestId = lastRequestId)
    }

    private fun probeIntent(targetPackage: String): Intent = Intent(GiftProtocol.ACTION_GIFT_TRANSACTION)
        .addCategory(Intent.CATEGORY_DEFAULT)
        .setPackage(targetPackage)

    private class IntentResultExtras(private val intent: Intent) : ResultExtras {
        override fun string(name: String): String? = intent.getStringExtra(name)
        override fun long(name: String): Long? =
            if (intent.hasExtra(name)) intent.getLongExtra(name, 0L) else null
    }
}

/** Whether your app can call Facilipay; see [FacilipayGift.availability]. */
public enum class Availability {
    /** Ready to use. */
    READY,

    /** Facilipay is not installed on this device. */
    NOT_INSTALLED,

    /** The installed Facilipay is too old for app integration. The merchant updates Facilipay. */
    NOT_SUPPORTED,

    /**
     * Your app does not hold Facilipay's permission. This happens when your app was installed
     * before Facilipay: reinstall or update your app now that Facilipay is installed.
     */
    PERMISSION_MISSING,
}
