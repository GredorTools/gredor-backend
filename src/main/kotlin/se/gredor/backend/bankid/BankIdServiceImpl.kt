package se.gredor.backend.bankid

import io.quarkus.arc.profile.UnlessBuildProfile
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import se.gredor.backend.auth.AuthService
import se.swedenconnect.bankid.rpapi.service.AuthenticateRequest
import se.swedenconnect.bankid.rpapi.service.BankIDClient
import se.swedenconnect.bankid.rpapi.service.UserVisibleData
import se.swedenconnect.bankid.rpapi.types.CollectResponse
import se.swedenconnect.bankid.rpapi.types.Requirement
import java.time.Duration
import java.time.Instant

@UnlessBuildProfile("dev")
@ApplicationScoped
class BankIdServiceImpl : BankIdService {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var authService: AuthService

    @Inject
    private lateinit var bankIdClient: BankIDClient

    @Inject
    private lateinit var bankIdOrderRepository: BankIdOrderRepository

    @Transactional
    override fun authInit(personalNumber: String, endUserIp: String): BankIdStatusResponse {
        // Grundläggande validering
        if (personalNumber.isBlank()) {
            throw IllegalArgumentException("personalNumber is required")
        }
        if (endUserIp.isBlank()) {
            throw IllegalArgumentException("endUserIp is required")
        }

        // Avbryt eventuella befintliga beställningar för samma personnummer och IP-adress
        bankIdOrderRepository.findByPersonalNumberAndEndUserIp(personalNumber, endUserIp).forEach {
            cancel(it.orderRef)
        }

        // Skapa BankID-autentiseringsbegäran
        val userVisibleData = UserVisibleData().apply {
            setDisplayText("Använda Gredor för att kontrollera och/eller ladda upp en årsredovisning.")
        }
        val requirement = Requirement().apply {
            this.personalNumber = personalNumber
        }
        val authenticateRequest = AuthenticateRequest(endUserIp, userVisibleData, requirement)

        // Anropa BankID autentiserings-API och blockera för resultatet
        val orderResponse = bankIdClient.authenticate(authenticateRequest).block()
            ?: throw RuntimeException("Failed to get response from BankID service")

        val orderRef = orderResponse.orderReference

        val bankIdOrderEntity = BankIdOrderEntity(
            orderRef = orderRef,
            personalNumber = personalNumber,
            endUserIp = endUserIp,
            qrStartToken = orderResponse.qrStartToken,
            qrStartSecret = orderResponse.qrStartSecret,
            orderTime = orderResponse.orderTime.toEpochMilli()
        )
        bankIdOrderEntity.persist()

        return BankIdStatusResponse(
            orderRef = orderRef,
            autoStartToken = orderResponse.autoStartToken,
            status = AuthStatus.PENDING,
            statusPendingData = StatusPendingData(
                qrCodeImageBase64 = generateQrCodeData(orderRef),
                hintCode = null,
            )
        )
    }

    override fun authStatus(orderRef: String): BankIdStatusResponse {
        // Grundläggande validering
        if (orderRef.isBlank()) {
            throw IllegalArgumentException("orderRef is required")
        }

        // Anropa BankID collect-API och blockera för resultat
        val collectResponse = bankIdClient.collect(orderRef).block()
            ?: throw RuntimeException("Failed to get response from BankID service")

        val response = when (collectResponse.status) {
            CollectResponse.Status.COMPLETE -> {
                val completionInfo = collectResponse.completionData
                    ?: throw RuntimeException("Missing completion data for completed authentication")

                cleanOrderFromDatabase(orderRef)

                BankIdStatusResponse(
                    status = AuthStatus.COMPLETE,
                    statusCompleteData = StatusCompleteData(
                        personalNumber = completionInfo.user.personalNumber,
                        token = authService.createToken(completionInfo.user.personalNumber)
                    )
                )
            }

            CollectResponse.Status.PENDING ->
                BankIdStatusResponse(
                    status = AuthStatus.PENDING,
                    statusPendingData = StatusPendingData(
                        qrCodeImageBase64 = generateQrCodeData(orderRef),
                        hintCode = collectResponse.hintCode
                    )
                )

            CollectResponse.Status.FAILED ->
                BankIdStatusResponse(
                    status = AuthStatus.FAILED,
                    statusFailedData = StatusFailedData(
                        hintCode = collectResponse.hintCode
                    )
                )
        }

        return response
    }

    override fun cancel(orderRef: String) {
        // Grundläggande validering
        if (orderRef.isBlank()) {
            throw IllegalArgumentException("orderRef is required")
        }

        // Anropa BankID avbryt-API
        bankIdClient.cancel(orderRef)
            .onErrorResume { Mono.empty() }
            .block()
    }

    @Transactional
    fun cleanOrderFromDatabase(orderRef: String) = bankIdOrderRepository.deleteByOrderRef(orderRef)

    @Scheduled(cron = "0 30 * * * ?")
    @Transactional
    fun cleanOldOrders() {
        val numDeleted = bankIdOrderRepository.deleteOldOrders(Duration.ofHours(1))
        logger.info("Deleted $numDeleted old order(s)")
    }

    private fun generateQrCodeData(orderRef: String): String {
        val bankIdOrderEntity = bankIdOrderRepository.findByOrderRef(orderRef)
        return generateQrCodeData(
            bankIdOrderEntity.qrStartToken,
            bankIdOrderEntity.qrStartSecret,
            Instant.ofEpochMilli(bankIdOrderEntity.orderTime)
        )
    }

    private fun generateQrCodeData(qrStartToken: String, qrStartSecret: String, orderTime: Instant): String {
        return bankIdClient.qrGenerator.generateAnimatedQRCodeBase64Image(qrStartToken, qrStartSecret, orderTime)
    }
}
