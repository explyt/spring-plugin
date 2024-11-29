package com.example

import jakarta.persistence.*
import javax.persistence.*

@Embeddable
class Address {
    var city: String? = null
    var firstLine: String? = null
    var secondLine: String? = null
}