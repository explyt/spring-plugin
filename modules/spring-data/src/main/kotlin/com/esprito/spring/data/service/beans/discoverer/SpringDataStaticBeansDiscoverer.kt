package com.esprito.spring.data.service.beans.discoverer

import com.esprito.spring.core.service.PsiBean
import com.esprito.spring.core.service.beans.discoverer.StaticBeansDiscoverer
import com.esprito.spring.data.SpringDataClasses
import com.esprito.spring.data.util.SpringDataUtil
import com.intellij.openapi.module.Module

class SpringDataStaticBeansDiscoverer : StaticBeansDiscoverer() {

    override fun accepts(module: Module): Boolean {
        return SpringDataUtil.isSpringDataJpaModule(module)
    }

    override fun discoverBeans(module: Module): Collection<PsiBean> {
        return listOfNotNull(
            getStaticBean(module, SpringDataClasses.JPA_CONTEXT, "jpaContext"),
        )
    }
}