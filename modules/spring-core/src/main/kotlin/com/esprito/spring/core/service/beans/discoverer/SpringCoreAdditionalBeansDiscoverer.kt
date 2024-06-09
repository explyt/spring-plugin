package com.esprito.spring.core.service.beans.discoverer

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.service.PsiBean
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