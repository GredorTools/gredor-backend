package se.gredor.backend.auth

import io.quarkus.hibernate.orm.panache.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant

@ApplicationScoped
class AuthRespository : PanacheRepository<AuthEntity> {
    fun countByPersonalNumber(personalNumber: String) =
        find("personalNumber = ?1", personalNumber).count()

    fun findByPersonalNumberAndToken(personalNumber: String, token: String) =
        find("personalNumber = ?1 AND token = ?2", personalNumber, token).firstResult<AuthEntity>()

    fun deleteExpired(): Long {
        val currentTimestamp = Instant.now().toEpochMilli()
        return delete("expiresAt < ?1", currentTimestamp)
    }
}
