package se.gredor.backend.bankid

interface BankIdService {
    /**
     * Initierar en BankID-autentisering för det givna personnumret.
     */
    fun authInit(personalNumber: String, endUserIp: String): BankIdStatusResponse

    /**
     * Hämtar status för en pågående BankID-autentisering.
     */
    fun authStatus(orderRef: String): BankIdStatusResponse

    /**
     * Avbryter en pågående BankID-autentisering.
     */
    fun cancel(orderRef: String)
}
