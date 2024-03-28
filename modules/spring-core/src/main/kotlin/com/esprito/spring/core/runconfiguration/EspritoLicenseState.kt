package com.esprito.spring.core.runconfiguration

enum class EspritoLicenseState(val state: Int) {
    Unknown(-1),
    NotValid(0),
    Valid(1),
    Expired(2),
    Empty(3),
    NotConnect(4)
}