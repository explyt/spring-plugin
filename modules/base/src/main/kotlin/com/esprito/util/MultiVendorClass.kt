package com.esprito.util

class MultiVendorClass(subFqn: String) {
    val javax: String = "javax.$subFqn"
    val jakarta: String = "jakarta.$subFqn"

    val allFqns by lazy { listOf(javax, jakarta) }

    fun check(fqn: String?): Boolean = fqn in allFqns
}