package se.gredor.backend.database

import io.quarkus.hibernate.orm.panache.PanacheEntityBase
import jakarta.persistence.*

@MappedSuperclass
abstract class BaseEntity : PanacheEntityBase() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    var id: Int? = null

    override fun toString(): String {
        return this.javaClass.getSimpleName() + "<" + id + ">"
    }
}
