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

package com.explyt.spring.data.service.beans.discoverer

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.RootDataHolder
import com.explyt.spring.core.service.beans.discoverer.AdditionalBeansDiscoverer
import com.explyt.spring.core.util.SpringCoreUtil.getQualifierAnnotation
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanName
import com.explyt.spring.data.SpringDataClasses
import com.explyt.spring.data.SpringDataClasses.SPRING_RESOURCE
import com.explyt.spring.data.service.SpringDataPackageScanService
import com.explyt.spring.data.util.SpringDataUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.search.searches.ClassInheritorsSearch

class SpringDataAdditionalBeansDiscoverer : AdditionalBeansDiscoverer() {

    override fun accepts(module: Module): Boolean {
        return SpringDataUtil.isSpringDataJpaModule(module)
    }

    override fun getExtraComponents(module: Module): Collection<PsiBean> {
        val repositoryPsiClass = LibraryClassCache.searchForLibraryClass(module, SPRING_RESOURCE) ?: return emptySet()
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