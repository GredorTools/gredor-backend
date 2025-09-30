package se.gredor.backend.bankid

interface BankIdService {
    fun authInit(personalNumber: String, endUserIp: String): BankIdStatusResponse

    fun authStatus(orderRef: String): BankIdStatusResponse

    fun cancel(orderRef: String)
}
