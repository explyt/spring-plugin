package com.explyt.spring.core.service.beans.discoverer

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.PsiBean
import com.intellij.openapi.module.Module

class SpringCoreAdditionalBeansDiscoverer : AdditionalBeansDiscoverer() {

    override fun discoverBeans(module: Module): Collection<PsiBean> {
        return listOfNotNull(
            getStaticBean(module, SpringCoreClasses.ENVIRONMENT, "environment"),
            getStaticBean(module, SpringCoreClasses.PROPERTY_RESOLVER, "propertyResolver"),
            getStaticBean(module, SpringCoreClasses.CONVERSION_SERVICE, "conversionService"),
            getStaticBean(module, SpringCoreClasses.APPLICATION_CONTEXT, "applicationContext"),
        )
    }

}