/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.data.service.beans.discoverer

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.RootDataHolder
import com.explyt.spring.core.service.beans.discoverer.AdditionalBeansDiscoverer
import com.explyt.spring.core.util.SpringCoreUtil.getQualifierAnnotation
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanName
import com.explyt.spring.data.SpringDataClasses
import com.explyt.spring.data.SpringDataClasses.REPOSITORY
import com.explyt.spring.data.service.SpringDataPackageScanService
import com.explyt.spring.data.util.SpringDataUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.search.searches.ClassInheritorsSearch

class SpringDataAdditionalBeansDiscoverer : AdditionalBeansDiscoverer() {

    override fun accepts(module: Module): Boolean {
        return SpringDataUtil.isSpringDataJpaModule(module)
    }

    override fun getExtraComponents(module: Module): Collection<PsiBean> {
        val repositoryPsiClass = LibraryClassCache.searchForLibraryClass(module, REPOSITORY) ?: return emptySet()
        val scope = module.moduleWithDependenciesScope
        return ClassInheritorsSearch.search(repositoryPsiClass, scope, true)
            .map { PsiBean(it.resolveBeanName(module), it, it.getQualifierAnnotation(), it) }
    }

    override fun discoverBeans(module: Module): Collection<PsiBean> {
        return listOfNotNull(
            getStaticBean(module, SpringDataClasses.JPA_CONTEXT, "jpaContext"),
        )
    }

    override fun additionalFilterBeans(module: Module, bean: PsiBean, rootDataHolder: RootDataHolder): Boolean {
        val packages = SpringDataPackageScanService.getInstance(module.project).getPackages(module)
        val qualifiedName = bean.psiClass.qualifiedName ?: return false
        return packages.any { qualifiedName.startsWith(it) }
    }
}