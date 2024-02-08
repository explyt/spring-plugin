package com.example

import jakarta.persistence.*
import javax.persistence.*

@Entity
@DiscriminatorValue("customer")
class Customer : BaseEntity() {
    @Column
    var name: String? = null

    @Column
    var email: String? = null
}