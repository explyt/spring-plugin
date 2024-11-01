package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.service.PsiBean
import com.intellij.psi.PsiMember

interface ExclusionStrategy {
    fun shouldExclude(dependant: PsiMember, foundBeans: Collection<PsiBean>): Boolean

}