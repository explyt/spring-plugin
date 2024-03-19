package com.esprito.spring.data.service.beans.discoverer

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.service.PsiBean
import com.esprito.spring.core.service.beans.discoverer.AdditionalBeansDiscoverer
import com.esprito.spring.core.util.SpringCoreUtil.getQualifierAnnotation
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanName
import com.esprito.spring.data.SpringDataClasses
import com.esprito.spring.data.SpringDataClasses.SPRING_RESOURCE
import com.esprito.spring.data.util.SpringDataUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.search.searches.ClassInheritorsSearch

class SpringDataAdditionalBeansDiscoverer : AdditionalBeansDiscoverer() {

    override fun accepts(module: Module): Boolean {
        return SpringDataUtil.isSpringDataJpaModule(module)
    }

    override fun getExtraComponents(module: Module): Collection<PsiBean> {
        val repositoryPsiClass = LibraryClassCache.searchForLibraryClass(module, SPRING_RESOURCE) ?: return emptySet()
        val scope = module.moduleWithDependenciesScope
        return ClassInheritorsSearch.search(repositoryPsiClass, scope, false)
            .map { PsiBean(it.resolveBeanName(module), it, it.getQualifierAnnotation(), it) }
    }

    override fun discoverBeans(module: Module): Collection<PsiBean> {
        return listOfNotNull(
            getStaticBean(module, SpringDataClasses.JPA_CONTEXT, "jpaContext"),
        )
    }
}