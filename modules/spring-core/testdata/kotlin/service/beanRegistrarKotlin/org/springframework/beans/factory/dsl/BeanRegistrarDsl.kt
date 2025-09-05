package org.springframework.beans.factory.dsl

open class BeanRegistrarDsl(init: () -> Unit) {
    init { init() }
}

inline fun <reified T: Any> registerBean(name: String? = null, noinline supplier: (() -> T)? = null) {}

fun profile(name: String, block: () -> Unit) { block() }
