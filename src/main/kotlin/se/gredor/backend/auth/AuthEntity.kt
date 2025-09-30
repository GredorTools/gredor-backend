package se.gredor.backend.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import se.gredor.backend.database.BaseEntity

@Entity
@Table(name = "GREDOR_AUTH")
class AuthEntity(

    @Column(name = "TOKEN", length = 36, nullable = false)
    var token: String,

    @Column(name = "PERSONAL_NUMBER", length = 12, nullable = false)
    var personalNumber: String,

    @Column(name = "ISSUED_AT", nullable = false)
    var issuedAt: Long,

    @Column(name = "EXPIRES_AT", nullable = false)
    var expiresAt: Long
    
) : BaseEntity()
