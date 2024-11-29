package com

import jakarta.persistence.*

@Entity
class ComOwner {
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
