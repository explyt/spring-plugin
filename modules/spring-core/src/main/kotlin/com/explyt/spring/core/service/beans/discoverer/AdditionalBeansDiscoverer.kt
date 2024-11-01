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

package com.explyt.spring.core.service.beans.discoverer

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.RootDataHolder
import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.openapi.module.Module

abstract class AdditionalBeansDiscoverer {

    open fun accepts(module: Module): Boolean = true

    abstract fun discoverBeans(module: Module): Collection<PsiBean>

    fun getStaticBean(module: Module, className: String, beanName: String): PsiBean? {
        val psiClass = LibraryClassCache.searchForLibraryClass(module, className) ?: return null

        return PsiBean(
            name = beanName,
            psiClass = psiClass,
            psiMember = psiClass
        )

    }

    open fun getExtraComponents(module: Module): Collection<PsiBean> = emptyList()

    open fun additionalFilterBeans(module: Module, bean: PsiBean, rootDataHolder: RootDataHolder): Boolean {
        if (rootDataHolder.isEmpty()) return true
        val packages = rootDataHolder.getPackages(module)
        if (packages.isEmpty()) return false
        val qualifiedName = bean.psiClass.qualifiedName ?: return false
        return packages.any { qualifiedName.startsWith(it) }
    }

    companion object {
        val EP_NAME = ProjectExtensionPointName<AdditionalBeansDiscoverer>(
            "com.explyt.spring.core.additionalBeansDiscoverer"
        )
    }

}