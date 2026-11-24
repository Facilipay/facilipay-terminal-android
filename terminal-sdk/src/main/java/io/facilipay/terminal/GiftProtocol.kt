package io.facilipay.terminal

/**
 * The wire names and values of Facilipay's gift intent. The shared ones are listed in
 * `protocol/gift-intent-protocol.properties` at the root of this repository, which Facilipay's app
 * checks itself against too; `GiftProtocolContractTest` fails if this object and the file differ.
 * The names match the file's keys.
 */
internal object GiftProtocol {
    const val ACTION_GIFT_TRANSACTION = "com.facilipay.pos.action.GIFT_TRANSACTION"
    const val PERMISSION_SUFFIX = ".permission.GIFT_INTEGRATION"

    /** The newest protocol version this library speaks. */
    const val API_VERSION = 2
    const val API_VERSION_1 = 1
    const val API_VERSION_2 = 2

    const val EXTRA_REQUEST_ID = "com.facilipay.pos.extra.REQUEST_ID"
    const val EXTRA_OPERATION = "com.facilipay.pos.extra.OPERATION"
    const val EXTRA_AMOUNT_MINOR = "com.facilipay.pos.extra.AMOUNT_MINOR"
    const val EXTRA_PAYMENT_COLLECTED = "com.facilipay.pos.extra.PAYMENT_COLLECTED"
    const val EXTRA_ALLOW_PARTIAL = "com.facilipay.pos.extra.ALLOW_PARTIAL"
    const val EXTRA_CURRENCY = "com.facilipay.pos.extra.CURRENCY"
    const val EXTRA_API_VERSION = "com.facilipay.pos.extra.API_VERSION"

    const val MAX_REQUEST_ID_LENGTH = 128
    const val MAX_AMOUNT_MINOR = 99_999_999L

    const val OPERATION_TOPUP = "TOPUP"
    const val OPERATION_REDEEM = "REDEEM"
    const val OPERATION_BALANCE = "BALANCE"

    const val RESULT_STATUS = "com.facilipay.pos.result.STATUS"
    const val RESULT_REQUEST_ID = "com.facilipay.pos.result.REQUEST_ID"
    const val RESULT_SEQUENCE_NO = "com.facilipay.pos.result.SEQUENCE_NO"
    const val RESULT_RESPONSE_CODE = "com.facilipay.pos.result.RESPONSE_CODE"
    const val RESULT_MESSAGE = "com.facilipay.pos.result.MESSAGE"

    /** Sent by Facilipay; the library doesn't read it (the operation is in the request). */
    const val RESULT_TRANSACTION_TYPE = "com.facilipay.pos.result.TRANSACTION_TYPE"
    const val RESULT_TRANSACTION_UID = "com.facilipay.pos.result.TRANSACTION_UID"
    const val RESULT_AMOUNT_MINOR = "com.facilipay.pos.result.AMOUNT_MINOR"
    const val RESULT_BALANCE_MINOR = "com.facilipay.pos.result.BALANCE_MINOR"
    const val RESULT_AUTH_CODE = "com.facilipay.pos.result.AUTH_CODE"
    const val RESULT_MASKED_PAN = "com.facilipay.pos.result.MASKED_PAN"
    const val RESULT_RECEIPT_TEXT = "com.facilipay.pos.result.RECEIPT_TEXT"

    const val RESULT_PAYMENT_STATUS = "com.facilipay.pos.result.PAYMENT_STATUS"
    const val RESULT_PAYMENT_RESPONSE_CODE = "com.facilipay.pos.result.PAYMENT_RESPONSE_CODE"
    const val RESULT_PAYMENT_REFERENCE = "com.facilipay.pos.result.PAYMENT_REFERENCE"
    const val RESULT_PAYMENT_AUTH_CODE = "com.facilipay.pos.result.PAYMENT_AUTH_CODE"
    const val RESULT_PAYMENT_MASKED_PAN = "com.facilipay.pos.result.PAYMENT_MASKED_PAN"
    const val RESULT_PAYMENT_AMOUNT_MINOR = "com.facilipay.pos.result.PAYMENT_AMOUNT_MINOR"
    const val RESULT_PAYMENT_RECEIPT_TEXT = "com.facilipay.pos.result.PAYMENT_RECEIPT_TEXT"

    const val PAYMENT_STATUS_NOT_TAKEN = "NOT_TAKEN"
    const val PAYMENT_STATUS_APPROVED = "APPROVED"
    const val PAYMENT_STATUS_DECLINED = "DECLINED"
    const val PAYMENT_STATUS_CANCELLED = "CANCELLED"
    const val PAYMENT_STATUS_UNKNOWN = "UNKNOWN"

    const val STATUS_APPROVED = "APPROVED"
    const val STATUS_CANCELLED = "CANCELLED"
    const val STATUS_ERROR = "ERROR"
    const val STATUS_UNKNOWN = "UNKNOWN"

    const val CODE_APPROVED = "00"
    const val CODE_PARTIAL = "10"
    const val CODE_MALFORMED = "30"
    const val CODE_UNKNOWN_OPERATION = "12"
    const val CODE_INVALID_AMOUNT = "13"
    const val CODE_NOT_ENABLED = "58"
    const val CODE_NOT_LOGGED_IN = "27"
    const val CODE_IN_PROGRESS = "09"
    const val CODE_DUPLICATE = "94"
    const val CODE_NOT_STARTED = "21"
    const val CODE_OUTCOME_UNKNOWN = "22"
    const val CODE_PAID_NOT_CREDITED = "PC"

    const val CODE_CANCELLED_ON_TERMINAL = "P3"
    const val CODE_CANCELLED_BY_ANOTHER_REQUEST = "P4"
    const val CODE_TIMED_OUT = "P5"

    const val CODE_INSUFFICIENT_BALANCE = "51"
    const val CODE_CARD_EXPIRED = "54"
    const val CODE_CARD_NOT_FOUND = "56"
    const val CODE_CARD_NOT_LINKED = "14"
}
