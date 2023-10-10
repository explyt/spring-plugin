package com.esprito.spring.core.service.conditional

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.esprito.spring.core.service.PsiBean
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMember

class ConditionalOnClassStrategy(module: Module) : ExclusionStrategy {
    private val annotationHolder = MetaAnnotationsHolder.of(module, SpringCoreClasses.CONDITIONAL_ON_CLASS)

    override fun shouldExclude(dependant: PsiMember, foundBeans: Collection<PsiBean>): Boolean {
        return false
        // TODO: ConditionalOnMissingClassStrategy
    }

}