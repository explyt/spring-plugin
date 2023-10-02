package com.esprito.spring.core

object SpringProperties {
    val stringQualifiers = listOf(SpringCoreClasses.QUALIFIER) + JavaEeClasses.NAMED.allFqns
    val stringInjects = listOf(JavaEeClasses.INJECT.allFqns, JavaEeClasses.RESOURCE.allFqns)
}
