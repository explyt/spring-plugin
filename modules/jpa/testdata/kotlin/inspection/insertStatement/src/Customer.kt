import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue("customer")
class Customer : BaseEntity() {
    @Column
    var name: String? = null

    @Column
    var email: String? = null
}