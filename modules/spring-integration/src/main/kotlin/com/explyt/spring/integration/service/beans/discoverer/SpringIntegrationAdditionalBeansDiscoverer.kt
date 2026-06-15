/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.integration.service.beans.discoverer

import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.beans.discoverer.AdditionalBeansDiscoverer
import com.explyt.spring.integration.SpringIntegrationClasses
import com.explyt.spring.integration.util.SpringIntegrationUtil
import com.intellij.openapi.module.Module

class SpringIntegrationAdditionalBeansDiscoverer : AdditionalBeansDiscoverer() {

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