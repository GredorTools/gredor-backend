package se.gredor.backend.bankid

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import se.gredor.backend.auth.AuthService
import se.swedenconnect.bankid.rpapi.service.BankIDClient
import se.swedenconnect.bankid.rpapi.service.QRGenerator
import se.swedenconnect.bankid.rpapi.types.CollectResponse
import se.swedenconnect.bankid.rpapi.types.CollectResponse.Status
import se.swedenconnect.bankid.rpapi.types.CompletionData
import se.swedenconnect.bankid.rpapi.types.OrderResponse
import java.time.Instant
import java.util.*


@QuarkusTest
class BankIdServiceImplTest {

    private val mockPnr = "198605082380"
    private val mockIp = "127.0.0.1"
    private val mockOrderRef = UUID.randomUUID().toString()
    private val mockStartToken = "mockStartToken"
    private val mockStartSecret = "mockStartSecret"

    @Inject
    lateinit var svc: BankIdService

    @Inject
    lateinit var repo: BankIdOrderRepository

    @InjectMock
    lateinit var bankIdClient: BankIDClient

    @InjectMock
    lateinit var authService: AuthService

    @Test
    fun authInit_blankParams_throw() {
        assertThrows(IllegalArgumentException::class.java) {
            svc.authInit("", mockIp)
        }
        assertThrows(IllegalArgumentException::class.java) {
            svc.authInit(mockPnr, "")
        }
    }

    @Test
    fun authStatus_blankOrderRef_throw() {
        assertThrows(IllegalArgumentException::class.java) {
            svc.authStatus("")
        }
    }

    @Test
    fun cancel_blankOrderRef_throw() {
        assertThrows(IllegalArgumentException::class.java) {
            svc.cancel("")
        }
    }

    @Test
    fun authInit_success_persists_andReturnsPendingWithQr() {
        // Mocka
        val orderTime = Instant.now()
        val orderResponse = mockk<OrderResponse>()

        val qrGen = mockk<QRGenerator>()
        every { bankIdClient.qrGenerator } returns qrGen
        every { qrGen.generateAnimatedQRCodeBase64Image(mockStartToken, mockStartSecret, any()) } returns "data:qr"

        every { orderResponse.orderReference } returns mockOrderRef
        every { orderResponse.qrStartToken } returns mockStartToken
        every { orderResponse.qrStartSecret } returns mockStartSecret
        every { orderResponse.orderTime } returns orderTime
        every { orderResponse.autoStartToken } returns "mockAutoStartToken"

        every { bankIdClient.authenticate(any()) } returns Mono.just(orderResponse)

        // Kör och verifiera
        val resp = svc.authInit(mockPnr, mockIp)

        assertEquals(BankIdStatus.PENDING, resp.status)
        assertEquals(mockOrderRef, resp.orderRef)
        assertEquals("mockAutoStartToken", resp.autoStartToken)
        assertEquals("data:qr", resp.statusPendingData?.qrCodeImageBase64)
        assertNull(resp.statusPendingData?.hintCode)

        val stored = repo.findByOrderRef(mockOrderRef)
        assertEquals(mockOrderRef, stored.orderRef)
        assertEquals(mockPnr, stored.personalNumber)
        assertEquals(mockStartToken, stored.qrStartToken)
        assertEquals(mockStartSecret, stored.qrStartSecret)
        assertEquals(orderTime.toEpochMilli(), stored.orderTime)
    }

    @Test
    @Transactional
    fun authStatus_pending_returnsPendingWithHintAndQr() {
        // Mocka
        val ent = BankIdOrderEntity(
            orderRef = mockOrderRef,
            personalNumber = mockPnr,
            endUserIp = mockIp,
            qrStartToken = mockStartToken,
            qrStartSecret = mockStartSecret,
            orderTime = Instant.now().toEpochMilli()
        )
        ent.persist()

        val qrGen = mockk<QRGenerator>()
        every { bankIdClient.qrGenerator } returns qrGen
        every {
            qrGen.generateAnimatedQRCodeBase64Image(
                mockStartToken,
                mockStartSecret,
                any()
            )
        } returns "data:qr"

        val collect = mockk<CollectResponse>()
        every { collect.status } returns Status.PENDING
        every { collect.hintCode } returns "mockHintCode"
        every { bankIdClient.collect(mockOrderRef) } returns Mono.just(collect)

        // Kör och verifiera
        val resp = svc.authStatus(mockOrderRef)
        assertEquals(BankIdStatus.PENDING, resp.status)
        assertEquals("data:qr", resp.statusPendingData?.qrCodeImageBase64)
        assertEquals("mockHintCode", resp.statusPendingData?.hintCode)
    }

    @Test
    fun authStatus_failed_returnsFailed() {
        // Mocka
        val collect = mockk<CollectResponse>()
        every { collect.status } returns Status.FAILED
        every { collect.hintCode } returns "CANCELLED"
        every { bankIdClient.collect(mockOrderRef) } returns Mono.just(collect)

        // Kör och verifiera
        val resp = svc.authStatus(mockOrderRef)
        assertEquals(BankIdStatus.FAILED, resp.status)
        assertEquals("CANCELLED", resp.statusFailedData?.hintCode)
    }

    @Test
    @Transactional
    fun authStatus_complete_createsToken_andCleansOrderInDatabase() {
        // Mocka
        val ent = BankIdOrderEntity(
            orderRef = mockOrderRef,
            personalNumber = mockPnr,
            endUserIp = mockIp,
            qrStartToken = mockStartToken,
            qrStartSecret = mockStartSecret,
            orderTime = Instant.now().toEpochMilli()
        )
        ent.persist()

        val completion = mockk<CompletionData>()
        every { completion.user.personalNumber } returns mockPnr

        val collect = mockk<CollectResponse>()
        every { collect.status } returns Status.COMPLETE
        every { collect.completionData } returns completion
        every { bankIdClient.collect(mockOrderRef) } returns Mono.just(collect)

        every { authService.createToken(mockPnr) } returns "mockToken"

        // Kör och verifiera
        val resp = svc.authStatus(mockOrderRef)

        assertEquals(BankIdStatus.COMPLETE, resp.status)
        assertEquals(mockPnr, resp.statusCompleteData?.personalNumber)
        assertEquals("mockToken", resp.statusCompleteData?.token)

        assertNull(repo.findByOrderRef(mockOrderRef))
    }

    @Test
    fun cancel_callsBankIdCancel() {
        // Mocka
        every { bankIdClient.cancel(any()) } returns Mono.empty()

        // Kör och verifiera
        assertDoesNotThrow { svc.cancel(mockOrderRef) }
        verify { bankIdClient.cancel(mockOrderRef) }
    }
}
