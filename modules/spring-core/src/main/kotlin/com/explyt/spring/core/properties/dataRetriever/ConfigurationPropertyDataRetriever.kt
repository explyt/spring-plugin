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

package com.explyt.spring.core.properties.dataRetriever

import ai.grazie.utils.capitalize
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.service.ConfigurationPropertiesService
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

abstract class ConfigurationPropertyDataRetriever {

    abstract fun getContainingClass(): PsiClass?

    abstract fun getMemberName(): String?

    abstract fun getNameElementPsi(): PsiElement?

    abstract fun isMap(): Boolean

    abstract fun isCollection(): Boolean

    fun getRelatedProperties(prefix: String, name: String, module: Module): List<PsiElement> {
        val propertyFqn = prefix + name
        return if (isMap() || isCollection()) {
            DefinedConfigurationPropertiesSearch.getInstance(module.project)
                .getAllProperties(module).asSequence()
                .filter {
                    PropertyUtil.toCommonPropertyForm(it.key)
                        .startsWith(PropertyUtil.toCommonPropertyForm(propertyFqn))
                }
                .mapNotNull { it.psiElement }.toList()
        } else {
            DefinedConfigurationPropertiesSearch.getInstance(module.project)
                .getAllProperties(module).asSequence()
                .filter { PropertyUtil.isSameProperty(it.key, propertyFqn) }
                .mapNotNull { it.psiElement }.toList()
        }
    }

    fun getMetadataName(prefix: String, name: String, module: Module): List<PsiElement> {
        val propertyFqn = prefix + name
        val hints = SpringConfigurationPropertiesSearch.getInstance(module.project)
            .getElementNameHints(module)

        return hints
            .asSequence()
            .filter { PropertyUtil.isSameProperty(it.name, propertyFqn) }
            .mapNotNull { it.jsonProperty.value }
            .toList()
    }

    companion object {

        fun getPrefixValue(psiClass: PsiClass): String {
            return CachedValuesManager.getManager(psiClass.project).getCachedValue(psiClass) {
                    CachedValueProvider.Result(
                        getPrefixFromUsage(psiClass),
                        ModificationTrackerManager.getInstance(psiClass.project).getUastModelAndLibraryTracker()
                    )
                }
        }

        private fun getPrefixFromUsage(psiClass: PsiClass): String {
            val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return ""
            if (psiClass.isMetaAnnotatedBy(SpringCoreClasses.CONFIGURATION_PROPERTIES)) {
                return ConfigurationPropertiesService.getPrefixFromAnnotation(psiClass, module)
            }
            val dataHolder = ConfigurationPropertiesService.getInstance(psiClass.project).getPrefixDataHolder()
            return dataHolder.getPrefix(psiClass)
        }
    }

    protected fun toPascalFormat(memberName: String?): String {
        return when {
            memberName == null -> ""
            memberName.startsWith("set") -> memberName.substring(3)
            memberName.startsWith("get") -> memberName.substring(3)
            else -> memberName.capitalize()
        }
    }

}