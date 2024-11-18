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

package com.explyt.spring.web.service.beans.discoverer

import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.beans.discoverer.AdditionalBeansDiscoverer
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.openapi.module.Module

class SpringWebAdditionalBeansDiscoverer : AdditionalBeansDiscoverer() {

    override fun accepts(module: Module): Boolean {
        return SpringWebUtil.isSpringWebModule(module)
    }

    override fun discoverBeans(module: Module): Collection<PsiBean> {
        return if (SpringWebUtil.hasJakartaClasses(module)) {
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
    }

}