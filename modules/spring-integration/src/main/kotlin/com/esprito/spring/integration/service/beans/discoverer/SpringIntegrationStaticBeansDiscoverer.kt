package com.esprito.spring.integration.service.beans.discoverer

import com.esprito.spring.core.service.PsiBean
import com.esprito.spring.core.service.beans.discoverer.StaticBeansDiscoverer
import com.esprito.spring.integration.SpringIntegrationClasses
import com.esprito.spring.integration.util.SpringIntegrationUtil
import com.intellij.openapi.module.Module

class SpringIntegrationStaticBeansDiscoverer : StaticBeansDiscoverer() {

    override fun accepts(module: Module): Boolean {
        return SpringIntegrationUtil.isSpringIntegrationModule(module)
    }

    override fun discoverBeans(module: Module): Collection<PsiBean> {
        return listOfNotNull(
            getStaticBean(module, SpringIntegrationClasses.NULL_CHANNEL, "nullChannel"),
            getStaticBean(module, SpringIntegrationClasses.DIRECT_CHANNEL, "errorChannel"),
            getStaticBean(module, SpringIntegrationClasses.INTEGRATION_FLOW_CONTEXT, "integrationFlowContext")
        )
    }

}