package com

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

interface I

@Component
internal class A : I

@Service
internal class B : I

@Configuration
open class DependsOnConfiguration {
    @DependsOn("a")
    @Bean
    open fun bDependentOnA(): I {
        return B()
    }

    @DependsOn("wrongBeanNameAtMethod") // Ultimate handles it wrong. There is no navigation, nor error bean highlight
    @Bean
    open fun bDependentOnWrong(): I {
        return B()
    }
}

//region Valid
@Component
@DependsOn("a")
internal class DependentOnABean

@Component
@DependsOn("a", "b", E.CONST_TO_VALID_BEAN)
internal class DependentOnMultipleBeans

//endregion
//region Invalid
@Component
@DependsOn("wrongBeanNameAtClass")
internal class DependentOnABeanErr

@Component
@DependsOn("wrongBeanNameListedAtClass", "a", E.CONST_TO_NON_EXISTING_BEAN)
internal class DependentOnMultipleBeansErr

//endregion
@Component
internal object E : I {
    const val CONST_TO_VALID_BEAN: String = "b"
    const val CONST_TO_NON_EXISTING_BEAN: String = "nope"
}