/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.service.beans.discoverer

import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.beans.discoverer.AdditionalBeansDiscoverer
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.openapi.module.Module

class SpringWebAdditionalBeansDiscoverer : AdditionalBeansDiscoverer() {

    override fun accepts(module: Module): Boolean {
        return SpringWebUtil.isWebModule(module)
    }

    override fun discoverBeans(module: Module): Collection<PsiBean> {
        val servletBeans = if (SpringWebUtil.hasJakartaClasses(module)) {
            listOfNotNull(
                getStaticBean(module, SpringWebClasses.JAKARTA_SERVLET_CONTEXT, "servletContext"),
                getStaticBean(module, SpringWebClasses.JAKARTA_SERVLET_CONFIG, "servletConfig"),
                getStaticBean(module, SpringWebClasses.JAKARTA_SERVLET_REQUEST, "httpServletRequest"),
                getStaticBean(module, SpringWebClasses.JAKARTA_HTTP_SERVLET_REQUEST, "httpServletRequest"),
                getStaticBean(module, SpringWebClasses.JAKARTA_HTTP_SERVLET_RESPONSE, "httpServletResponse"),
                getStaticBean(module, SpringWebClasses.JAKARTA_HTTP_SESSION, "httpSession"),
                getStaticBean(module, SpringWebClasses.WEB_APPLICATION_CONTEXT, "webApplicationContext"),
            )
        } else {
            listOfNotNull(
                getStaticBean(module, SpringWebClasses.JAVAX_SERVLET_CONTEXT, "servletContext"),
                getStaticBean(module, SpringWebClasses.JAVAX_SERVLET_CONFIG, "servletConfig"),
                getStaticBean(module, SpringWebClasses.JAVAX_SERVLET_REQUEST, "httpServletRequest"),
                getStaticBean(module, SpringWebClasses.JAVAX_HTTP_SERVLET_REQUEST, "httpServletRequest"),
                getStaticBean(module, SpringWebClasses.JAVAX_HTTP_SERVLET_RESPONSE, "httpServletResponse"),
                getStaticBean(module, SpringWebClasses.JAVAX_HTTP_SESSION, "httpSession"),
                getStaticBean(module, SpringWebClasses.WEB_APPLICATION_CONTEXT, "webApplicationContext"),
            )
        }

        return servletBeans + testClientBeans(module)
    }

    /**
     * Beans contributed by Spring Boot's *test* auto-configuration (`spring-boot-test-autoconfigure`), which is not
     * part of the regular auto-configuration model: `@AutoConfigureMockMvc` and the slice annotations such as
     * `@WebMvcTest` register them, so autowiring them in a test is valid.
     *
     * `getStaticBean` resolves the class in the module scope and returns `null` when it is absent, so listing a type
     * here has no effect on modules without the corresponding test dependency.
     */
    private fun testClientBeans(module: Module): List<PsiBean> = listOfNotNull(
        getStaticBean(module, SpringWebClasses.MOCK_MVC, "mockMvc"),
    )

}
