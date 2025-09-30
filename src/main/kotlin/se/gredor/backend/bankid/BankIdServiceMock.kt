package se.gredor.backend.bankid

import io.quarkus.arc.profile.IfBuildProfile
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import se.gredor.backend.auth.AuthService
import se.swedenconnect.bankid.rpapi.types.CollectResponse
import java.util.*

@IfBuildProfile("dev")
@ApplicationScoped
class BankIdServiceMock : BankIdService {
    private companion object {
        val orderRefToPersonalNumber = HashMap<String, String?>()
    }

    private val mockQrCode =
        "data:image/png;base64,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"

    @Inject
    private lateinit var authService: AuthService

    override fun authInit(personalNumber: String, endUserIp: String): BankIdStatusResponse {
        val orderRef = UUID.randomUUID().toString()
        orderRefToPersonalNumber[orderRef] = personalNumber

        return BankIdStatusResponse(
            orderRef = orderRef,
            autoStartToken = UUID.randomUUID().toString(),
            status = AuthStatus.PENDING,
            statusPendingData = StatusPendingData(
                qrCodeImageBase64 = mockQrCode,
                hintCode = null
            ),
        )
    }

    override fun authStatus(orderRef: String): BankIdStatusResponse {
        val personalNumber = orderRefToPersonalNumber[orderRef]!!

        val collectResponseStatus =
            if (Math.random() > 0.75) CollectResponse.Status.COMPLETE else CollectResponse.Status.PENDING

        val response = when (collectResponseStatus) {
            CollectResponse.Status.COMPLETE ->
                BankIdStatusResponse(
                    status = AuthStatus.COMPLETE,
                    statusCompleteData = StatusCompleteData(
                        personalNumber = personalNumber,
                        token = authService.createToken(personalNumber),
                    ),
                )

            CollectResponse.Status.PENDING ->
                BankIdStatusResponse(
                    status = AuthStatus.PENDING,
                    statusPendingData = StatusPendingData(
                        qrCodeImageBase64 = mockQrCode,
                        hintCode = null
                    ),
                )

            else -> throw RuntimeException("This should never happen")
        }

        return response
    }

    override fun cancel(orderRef: String) {}
}
