package com.esprito.spring.core.references.contributors

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.references.SpringScopeReference
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UExpression

class SpringScopeReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val injection = injectionHostUExpression()

        val referenceProvider = ScopeReferenceProvider()
        registrar.registerUastReferenceProvider(
            injection.annotationParam(SpringCoreClasses.SCOPE, "value"), referenceProvider
        )
        registrar.registerUastReferenceProvider(
            injection.annotationParam(SpringCoreClasses.SCOPE, "scopeName"), referenceProvider
        )
    }
}

private class ScopeReferenceProvider : UastInjectionHostReferenceProvider() {
    override fun getReferencesForInjectionHost(
        uExpression: UExpression, host: PsiLanguageInjectionHost, context: ProcessingContext
    ): Array<PsiReference> = arrayOf(SpringScopeReference(host))
}