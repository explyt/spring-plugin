/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.references.contributors

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.providers.PackageAntReferenceInAnnotationProvider
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.patterns.StandardPatterns
import com.intellij.patterns.uast.capture
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.registerUastReferenceProvider
import org.jetbrains.uast.UAnnotation

class PackageAntReferenceInAnnotationContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(
            StandardPatterns.or(
                injectionHostUExpression(false).annotationParams(
                    capture(UAnnotation::class.java),
                    StandardPatterns.string().oneOf(
                        SpringCoreUtil.BASE_PACKAGES, SpringCoreUtil.SCAN_BASE_PACKAGES
                    )
                ),
                injectionHostUExpression(false).annotationParam(
                    SpringCoreClasses.CONFIGURATION_PROPERTIES_SCAN, "value"
                ),
                injectionHostUExpression(false).annotationParam(
                    SpringCoreClasses.COMPONENT_SCAN, "value"
                )
            ),
            PackageAntReferenceInAnnotationProvider()
        )
    }

}