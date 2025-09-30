package se.gredor.backend.bankid

data class BankIdStatusResponse(
    val orderRef: String? = null, // Only available on init response
    val autoStartToken: String? = null, // Only available on init response
    val status: AuthStatus,
    val statusPendingData: StatusPendingData? = null,
    val statusCompleteData: StatusCompleteData? = null,
    val statusFailedData: StatusFailedData? = null
)

enum class AuthStatus {
    PENDING,
    COMPLETE,
    FAILED
}

data class StatusPendingData(
    val qrCodeImageBase64: String,
    val hintCode: String?
)

data class StatusCompleteData(
    val personalNumber: String,
    val token: String
)

data class StatusFailedData(
    val hintCode: String?
)


