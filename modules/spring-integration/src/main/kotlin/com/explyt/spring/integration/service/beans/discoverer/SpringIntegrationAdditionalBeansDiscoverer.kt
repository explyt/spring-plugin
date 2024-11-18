/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
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