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