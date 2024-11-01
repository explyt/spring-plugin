package com.explyt.spring.core.service.beans.discoverer

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.openapi.module.Module

class SpringBootAdditionalBeansDiscoverer : AdditionalBeansDiscoverer() {

    override fun accepts(module: Module): Boolean {
        return SpringCoreUtil.hasBootLibrary(module)
    }

    override fun discoverBeans(module: Module): Collection<PsiBean> {
        return listOfNotNull(
            getStaticBean(module, SpringCoreClasses.APPLICATION_ARGUMENTS, "springApplicationArguments")
        )
    }

}