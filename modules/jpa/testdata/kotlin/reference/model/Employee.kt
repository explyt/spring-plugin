package com.example

import jakarta.persistence.*
import javax.persistence.*

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
}
