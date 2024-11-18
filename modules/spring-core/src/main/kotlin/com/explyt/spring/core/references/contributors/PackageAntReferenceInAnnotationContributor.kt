/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
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