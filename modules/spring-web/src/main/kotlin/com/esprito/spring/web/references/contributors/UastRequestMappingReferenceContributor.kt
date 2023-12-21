package com.esprito.spring.web.references.contributors

import com.esprito.spring.core.references.contributors.CommonAnnotationReferenceProvider
import com.esprito.spring.web.SpringWebClasses
import com.esprito.spring.web.references.EspritoPathVariableReference
import com.esprito.spring.web.util.SpringWebUtil
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.*

class UastRequestMappingReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(injectionHostUExpression(), PathVariableReferenceProvider())
    }
}

class PathVariableReferenceProvider : CommonAnnotationReferenceProvider(annotationToMethodProperties) {

    override fun getReference(
        host: PsiLanguageInjectionHost,
        valueText: String,
        rangeInElement: TextRange
    ): PsiReference = EspritoPathVariableReference(host, valueText, rangeInElement)

    override fun getReferences(
        host: PsiLanguageInjectionHost,
        valueText: String,
        rangeInElement: TextRange
    ): Collection<PsiReference> {
        val matches = SpringWebUtil.NameInBracketsRx.findAll(valueText)

        return matches
            .mapNotNull { it.groups["name"] }
            .mapTo(mutableListOf()) {
                val range = TextRange(rangeInElement.startOffset + it.range.first, it.range.last + 2)
                getReference(host, it.value, range)
            }
    }

    companion object {
        val annotationToMethodProperties = mapOf(
            SpringWebClasses.REQUEST_MAPPING to setOf("path", "value"),
        )
    }

}