import jakarta.persistence.*

@Entity
class Owner {
    constructor()
    constructor(name: String?) {
        this.name = name
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column
    var name: String? = null
}
