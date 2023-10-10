package com.esprito.spring.core.service.conditional

import com.esprito.spring.core.service.PsiBean
import com.intellij.psi.PsiMember

interface ExclusionStrategy {
    fun shouldExclude(dependant: PsiMember, foundBeans: Collection<PsiBean>): Boolean

}