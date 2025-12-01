package se.gredor.backend.auth

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.*

@QuarkusTest
class AuthServiceTest {

    val mockPnr = "198605082380"

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var authRespository: AuthRespository

    @BeforeEach
    @Transactional
    fun setUp() {
        authRespository.deleteAll()
    }

    @Test
    fun createAndVerifyToken_success() {
        val token = authService.createToken(mockPnr)
        assertNotNull(token)
        assertTrue(token.length >= 10)
        assertTrue(authService.verifyToken(mockPnr, token))
    }

    @Test
    fun verifyToken_nonExistent_throws() {
        val token = UUID.randomUUID().toString()

        assertThrows(io.quarkus.security.UnauthorizedException::class.java) {
            authService.verifyToken(mockPnr, token)
        }
    }

    @Test
    @Transactional
    fun verifyToken_expired_throws() {
        val token = authService.createToken(mockPnr)

        // Ändra i databasen så att denna token har gått ut
        val ent = authRespository.find("personalNumber = ?1 AND token = ?2", mockPnr, token).firstResult<AuthEntity>()
        ent!!.expiresAt = Instant.now().minus(Duration.ofHours(1)).toEpochMilli()
        ent.persist()

        assertThrows(io.quarkus.security.UnauthorizedException::class.java) {
            authService.verifyToken(mockPnr, token)
        }
    }

    @Test
    fun isWithinAuthLimit_belowLimit_returnsTrue() {
        repeat(2) { authService.createToken(mockPnr) }
        assertTrue(authService.isWithinAuthLimit(mockPnr))
    }

    @Test
    fun isWithinAuthLimit_aboveLimit_returnsFalse() {
        repeat(20) { authService.createToken(mockPnr) }
        assertFalse(authService.isWithinAuthLimit(mockPnr))
    }

    @Test
    @Transactional
    fun cleanOldTokens_deletesExpired() {
        val tokenToBeDeleted = authService.createToken(mockPnr)

        // Ändra i databasen så att denna token har gått ut
        val entToBeDeleted = authRespository.find("personalNumber = ?1 AND token = ?2", mockPnr, tokenToBeDeleted)
            .firstResult<AuthEntity>()
        entToBeDeleted!!.expiresAt = Instant.now().minus(Duration.ofHours(2)).toEpochMilli()
        entToBeDeleted.persist()

        assertEquals(1, authRespository.count())
        authService.cleanOldTokens()
        assertEquals(0, authRespository.count())
    }

    @Test
    @Transactional
    fun cleanOldTokens_keepsNonexpired() {
        authService.createToken(mockPnr)

        assertEquals(1, authRespository.count())
        authService.cleanOldTokens()
        assertEquals(1, authRespository.count())
    }
}
