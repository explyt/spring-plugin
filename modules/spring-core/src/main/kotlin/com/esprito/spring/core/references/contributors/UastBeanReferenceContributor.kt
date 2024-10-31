package com.esprito.spring.core.references.contributors

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.references.ExplytBeanReference
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.*

class UastBeanReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(injectionHostUExpression(), BeanReferenceProvider())
    }
}

class BeanReferenceProvider : CommonAnnotationReferenceProvider(annotationToBeanProperties) {

    override fun getReference(
        host: PsiLanguageInjectionHost,
        valueText: String,
        rangeInElement: TextRange
    ): PsiReference = ExplytBeanReference(host, valueText, rangeInElement)

    companion object {
        val annotationToBeanProperties = mapOf(
            SpringCoreClasses.DEPENDS_ON to setOf("value"),
            SpringCoreClasses.LOOKUP to setOf("value"),
            SpringCoreClasses.CACHEABLE to setOf("value", "cacheNames", "keyGenerator", "cacheManager", "cacheResolver"),
            SpringCoreClasses.CONDITIONAL_ON_BEAN to setOf("name"),
            SpringCoreClasses.CONDITIONAL_ON_MISSING_BEAN to setOf("name"),
            SpringCoreClasses.BEAN to setOf("value", "name")
        )
    }

}


