package se.gredor.backend.bankid

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@QuarkusTest
class BankIdServiceImplTest {

    @Inject
    lateinit var svc: BankIdService

    @Test
    fun authInit_blankParams_throw() {
        // We only test validation, so blank params should throw before any deps are needed
        assertThrows(IllegalArgumentException::class.java) {
            svc.authInit("", "127.0.0.1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            svc.authInit("198605082380", "")
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
}
