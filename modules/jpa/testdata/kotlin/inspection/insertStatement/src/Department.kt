import jakarta.persistence.*

@Entity
class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column
    var name: String? = null

    @OneToMany(mappedBy = "department")
    var employees: List<Employee>? = null

    @Embedded
    var address: Address? = null
}
