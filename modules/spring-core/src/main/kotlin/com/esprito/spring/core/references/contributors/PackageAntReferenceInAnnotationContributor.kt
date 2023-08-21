package com.esprito.spring.core.references.contributors

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.providers.PackageAntReferenceInAnnotationProvider
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class PackageAntReferenceInAnnotationContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(),
            PackageAntReferenceInAnnotationProvider(
                SpringCoreClasses.ANNOTATIONS_WITH_PACKAGE_ANT_REFERENCES,
            )
        )
    }
}