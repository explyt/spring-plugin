import jakarta.persistence.Embeddable

@Embeddable
class Address {
    var city: String? = null
    var firstLine: String? = null
    var secondLine: String? = null
}