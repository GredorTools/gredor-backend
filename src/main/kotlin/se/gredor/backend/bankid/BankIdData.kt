package se.gredor.backend.bankid

data class BankIdStatusResponse(
    val orderRef: String? = null, // Only available on init response
    val autoStartToken: String? = null, // Only available on init response
    val status: BankIdStatus,
    val statusPendingData: BankIdStatusPendingData? = null,
    val statusCompleteData: BankIdStatusCompleteData? = null,
    val statusFailedData: BankIdStatusFailedData? = null
)

enum class BankIdStatus {
    PENDING,
    COMPLETE,
    FAILED
}

data class BankIdStatusPendingData(
    val qrCodeImageBase64: String,
    val hintCode: String?
)

data class BankIdStatusCompleteData(
    val personalNumber: String,
    val token: String
)

data class BankIdStatusFailedData(
    val hintCode: String?
)


