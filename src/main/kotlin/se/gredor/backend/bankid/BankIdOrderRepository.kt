package se.gredor.backend.bankid

import io.quarkus.hibernate.orm.panache.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import java.time.Duration
import java.time.Instant

@ApplicationScoped
class BankIdOrderRepository : PanacheRepository<BankIdOrderEntity> {
    fun findByOrderRef(orderRef: String) =
        find("orderRef = ?1", orderRef).firstResult<BankIdOrderEntity>()

    fun findByPersonalNumberAndEndUserIp(personalNumber: String, endUserIp: String) =
        find("personalNumber = ?1 and endUserIp = ?2", personalNumber, endUserIp).list<BankIdOrderEntity>()

    fun deleteByOrderRef(orderRef: String) =
        delete("orderRef = ?1", orderRef)

    fun deleteOldOrders(maxAge: Duration) = delete("orderTime < ?1", Instant.now().minus(maxAge).toEpochMilli())
}
