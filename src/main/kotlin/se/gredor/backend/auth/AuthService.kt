package se.gredor.backend.auth

import io.quarkus.scheduler.Scheduled
import io.quarkus.security.UnauthorizedException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.gredor.backend.config.AuthConfig
import java.time.Duration
import java.time.Instant
import java.util.*

@ApplicationScoped
class AuthService {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var authRespository: AuthRespository

    @Inject
    private lateinit var authConfig: AuthConfig

    @Transactional
    fun createToken(personalNumber: String): String {
        val token = UUID.randomUUID().toString()

        val authEntity = AuthEntity(
            token = token,
            personalNumber = personalNumber,
            issuedAt = Instant.now().toEpochMilli(),
            expiresAt = Instant.now().plus(Duration.ofHours(authConfig.intervalInHours())).toEpochMilli()
        )
        authEntity.persist()

        return token
    }

    @Transactional
    fun verifyToken(personalNumber: String, token: String): Boolean {
        authRespository.findByPersonalNumberAndToken(personalNumber, token) ?: throw UnauthorizedException()
        return true
    }

    @Transactional
    fun isWithinAuthLimit(personalNumber: String) =
        authRespository.count() < authConfig.limitPerInterval() &&
                authRespository.countByPersonalNumber(personalNumber) < authConfig.limitPerIntervalAndPerson()

    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    fun cleanOldTokens() {
        val numDeleted = authRespository.deleteExpired()
        logger.info("Deleted $numDeleted old auth token(s)")
    }
}
