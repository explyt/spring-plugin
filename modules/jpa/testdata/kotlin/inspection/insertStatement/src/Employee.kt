import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@DiscriminatorValue("employee")
class Employee : BaseEntity() {
    @Column
    var name: String? = null

    @Column
    var position: String? = null

    @ManyToOne
    @JoinColumn(name = "department_id")
    var department: Department? = null

    @Column
    var salary: BigDecimal? = null

    @Column
    var birthday: LocalDate? = null

    @Column
    var inVacation: Boolean? = null
}
