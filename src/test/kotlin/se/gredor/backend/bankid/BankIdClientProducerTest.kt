package se.gredor.backend.bankid

import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import se.swedenconnect.bankid.rpapi.service.impl.BankIDClientImpl

class BankIdClientProducerTest {

    lateinit var producer: BankIdClientProducer

    @MockK
    lateinit var bankIdConfig: BankIdConfig

    @BeforeEach
    fun setup() {
        producer = BankIdClientProducer()

        val field = BankIdClientProducer::class.java.getDeclaredField("bankIdConfig")
        field.isAccessible = true
        field.set(producer, bankIdConfig)
    }

    @Test
    fun produceBankIdClient_testMode_createsClient() {
        // Mocka
        every { bankIdConfig.clientIdentifier() } returns "test-client-id"
        every { bankIdConfig.certPath() } returns javaClass.classLoader.getResource("FPTestcert5_20240610.p12")!!.path
        every { bankIdConfig.certPassword() } returns "qwerty123"
        every { bankIdConfig.testMode() } returns true

        // Kör och verifiera
        val client = producer.produceBankIdClient()

        assertNotNull(client)
        assertTrue(client is BankIDClientImpl)
    }
}
