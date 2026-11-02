/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.trace

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.web.SpringWebClasses
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.psi.PsiClass

enum class SpringLayer { CONTROLLER, SERVICE, REPOSITORY, CONFIGURATION, COMPONENT }

fun layerOf(psiClass: PsiClass): SpringLayer? = when {
    psiClass.isMetaAnnotatedBy(SpringWebClasses.REST_CONTROLLER) -> SpringLayer.CONTROLLER
    psiClass.isMetaAnnotatedBy(SpringCoreClasses.CONTROLLER) -> SpringLayer.CONTROLLER
    psiClass.isMetaAnnotatedBy(SpringCoreClasses.SERVICE) -> SpringLayer.SERVICE
    psiClass.isMetaAnnotatedBy(SpringCoreClasses.REPOSITORY) -> SpringLayer.REPOSITORY
    psiClass.isMetaAnnotatedBy(SpringCoreClasses.CONFIGURATION) -> SpringLayer.CONFIGURATION
    psiClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT) -> SpringLayer.COMPONENT
    else -> null
}
