package se.gredor.backend.bankid

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import se.gredor.backend.database.BaseEntity

@Entity
@Table(name = "GREDOR_BANKID_ORDER")
class BankIdOrderEntity(

    @Column(name = "ORDER_REF", length = 36, nullable = false)
    var orderRef: String,

    @Column(name = "PERSONAL_NUMBER", length = 12, nullable = false)
    var personalNumber: String,

    @Column(name = "END_USER_IP", length = 45, nullable = false)
    var endUserIp: String,

    @Column(name = "QR_START_TOKEN", length = 36, nullable = false)
    var qrStartToken: String,

    @Column(name = "QR_START_SECRET", length = 36, nullable = false)
    var qrStartSecret: String,

    @Column(name = "ORDER_TIME", nullable = false)
    var orderTime: Long
    
) : BaseEntity()
