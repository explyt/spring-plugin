/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.service.PsiBean
import com.intellij.psi.PsiMember

interface ExclusionStrategy {
    fun shouldExclude(dependant: PsiMember, foundBeans: Collection<PsiBean>): Boolean

}