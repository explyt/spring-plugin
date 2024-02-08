package com

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

interface I {
    fun existingMethod()
}

abstract class A : I

@Component
class B : A() {
    override fun existingMethod() {}
}

@Configuration
open class MethodsInitDestroyConfiguration {
    @Bean
    open fun i(): I {
        return B()
    }

    @Bean
    open fun i1(): I {
        return B()
    }

    @Bean(initMethod = "")
    open fun i2(): I {
        return B()
    }

    @Bean(initMethod = "")
    open fun i3(): I {
        return B()
    }

    @Bean(destroyMethod = "")
    open fun i4(): I {
        return B()
    }

    @Bean(destroyMethod = EXISTING_METHOD)
    open fun i5(): I {
        return B()
    }

    @Bean(destroyMethod = NOT_EXISTING_METHOD)
    open fun i6(): I {
        return B()
    } //expecting error

    @Bean(initMethod = EXISTING_METHOD, destroyMethod = EXISTING_METHOD)
    open fun a(): A {
        return B()
    }

    @Bean
    open fun b(): B {
        return B()
    }

    @Bean(initMethod = EXISTING_METHOD)
    open fun b1(): B {
        return B()
    }

    @Bean(initMethod = NOT_EXISTING_METHOD)
    open fun b2(): B {
        return B()
    } //expecting error

    @Bean(destroyMethod = "foo")
    open fun b3(): B {
        return B()
    } //expecting error

    companion object {
        private const val EXISTING_METHOD = "existingMethod"
        private const val NOT_EXISTING_METHOD = "notExistingMethod"
    }
}