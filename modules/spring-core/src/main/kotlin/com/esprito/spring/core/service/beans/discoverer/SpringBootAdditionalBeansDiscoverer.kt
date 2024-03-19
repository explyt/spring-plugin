package com.esprito.spring.core.service.beans.discoverer

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.service.PsiBean
import com.esprito.spring.core.util.SpringCoreUtil
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