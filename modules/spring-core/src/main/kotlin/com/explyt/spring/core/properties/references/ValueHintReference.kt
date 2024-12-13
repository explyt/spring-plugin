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

package com.explyt.spring.core.properties.references

import com.explyt.spring.core.JavaCoreClasses
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.SpringProperties.VALUES
import com.explyt.spring.core.completion.properties.PropertyHint
import com.explyt.spring.core.completion.properties.PropertyValueRenderer
import com.explyt.spring.core.completion.properties.ProviderHint
import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.properties.ClassReferencePropertyRenderer
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.PropertyUtil.propertyKey
import com.explyt.spring.core.util.PropertyUtil.propertyValue
import com.explyt.util.ExplytKotlinUtil.mapToList
import com.explyt.util.ExplytPsiUtil.isPrivate
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.xml.util.documentation.MimeTypeDictionary
import java.nio.charset.Charset
import java.text.DateFormat

class ValueHintReference(
    element: PsiElement,
    textRange: TextRange,
) : PsiReferenceBase.Poly<PsiElement>(element, textRange, false) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val propertyKey = element.propertyKey() ?: return emptyArray()
        val propertyValue = element.propertyValue() ?: return emptyArray()
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()

        val propertyHint = PropertyUtil.getPropertyHint(module, propertyKey)
        val resolveResults = propertyHint?.providers?.flatMap {
            when (it.name) {
                SpringProperties.CLASS_REFERENCE -> getClassReference(module, propertyValue)
                SpringProperties.SPRING_BEAN_REFERENCE -> getSpringBeanReference(module, propertyValue)
                else -> emptyList()
            }
        } ?: emptyList()

        if (resolveResults.isNotEmpty()) {
            return resolveResults.map { PsiElementResolveResult(it) }.toTypedArray()
        }

        val javaPsiFacade = JavaPsiFacade.getInstance(module.project)
        val configurationProperty = SpringConfigurationPropertiesSearch.getInstance(module.project)
            .findProperty(module, propertyKey) ?: return emptyArray()
        val propertyType = configurationProperty.type?.replace('$', '.') ?: return emptyArray()
        val propertyTypeClass = javaPsiFacade.findClass(propertyType, GlobalSearchScope.allScope(module.project))
        if (propertyTypeClass?.isEnum == true) {
            val field = propertyTypeClass.findFieldByName(propertyValue, true) ?: return emptyArray()
            return arrayOf(PsiElementResolveResult(field))
        }
        return emptyArray()
    }

    private fun getSpringBeanReference(module: Module, propertyValue: String): List<PsiElement> {
        val springSearchService = SpringSearchServiceFacade.getInstance(element.project)
        return springSearchService.findActiveBeanDeclarations(module, propertyValue, element.language)
    }

    private fun getClassReference(module: Module, propertyValue: String): List<PsiElement> {
        val project = module.project
        val findClass = JavaPsiFacade.getInstance(project)
            .findClass(propertyValue, GlobalSearchScope.allScope(project)) ?: return emptyList()
        return listOf(findClass)
    }

    override fun getVariants(): Array<Any> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val propertyKey = element.propertyKey() ?: return emptyArray()
        val propertyHint = PropertyUtil.getPropertyHint(module, propertyKey)

        if (isMapWithoutValue(propertyKey, propertyHint)) {
            return emptyArray()
        }

        val result = mutableListOf<Any>()
        if (propertyHint != null) {
            result.addAll(getVariantsByHint(propertyHint))
        }

        val configurationProperty =
            SpringConfigurationPropertiesSearch.getInstance(module.project).findProperty(module, propertyKey)
        configurationProperty?.type?.replace('$', '.')?.let {
            result.addAll(getVariantsByPropertyType(it))
        }

        return result.toTypedArray()
    }

    private fun getVariantsByHint(
        propertyHint: PropertyHint
    ): List<Any> {
        val result = mutableListOf<Any>()
        result.addAll(propertyHint.values
            .filter { it.value != null }
            .map { LookupElementBuilder.create(it, it.value!!).withRenderer(PropertyValueRenderer()) })

        propertyHint.providers.flatMapTo(result) { providerHint ->
            processProviderHints(providerHint)
        }
        return result
    }

    private fun getVariantsByPropertyType(propertyType: String): List<Any> {
        return when (propertyType) {
            JavaCoreClasses.LOCALE -> {
                DateFormat.getAvailableLocales()
                    .mapNotNull {
                        val country = it.country
                        it.language + if (country.isNullOrBlank()) "" else "_$country"
                    }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .map {
                        LookupElementBuilder.create(it)
                    }
            }

            JavaCoreClasses.CHARSET -> {
                Charset.availableCharsets().map {
                    LookupElementBuilder.create(it.key)
                }
            }

            SpringCoreClasses.MIME_TYPE -> {
                MimeTypeDictionary.HTML_CONTENT_TYPES.map {
                    LookupElementBuilder.create(it)
                }
            }

            JavaCoreClasses.BOOLEAN, "boolean" -> {
                listOf("true", "false").map {
                    LookupElementBuilder.create(it)
                }
            }

            else -> {
                val project = element.project
                val propertyTypeClass = JavaPsiFacade.getInstance(project)
                    .findClass(propertyType, GlobalSearchScope.allScope(project))
                if (propertyTypeClass?.isEnum == true) {
                    return propertyTypeClass.fields.asSequence()
                        .filter { !it.isPrivate }
                        .mapToList { LookupElementBuilder.create(it) }
                }
                emptyList()
            }
        }
    }

    private fun processProviderHints(provider: ProviderHint): List<Any> {
        val targetClassFqn = provider.parameters?.target ?: return emptyList()
        when (provider.name) {
            SpringProperties.CLASS_REFERENCE -> {
                return getClassReferences(targetClassFqn)
            }

            SpringProperties.HANDLE_AS -> {
                return getVariantsByPropertyType(targetClassFqn)
            }

            SpringProperties.SPRING_BEAN_REFERENCE -> {
                return getBeanReferences(targetClassFqn)
            }
        }

        return emptyList()
    }

    private fun getClassReferences(targetClassFqn: String): List<Any> {
        val scope = GlobalSearchScope.allScope(element.project)
        val targetPsiClass =
            JavaPsiFacade.getInstance(element.project).findClass(targetClassFqn, scope) ?: return emptyList()
        return ClassInheritorsSearch.search(targetPsiClass, scope, true).asSequence()
            .plus(targetPsiClass)
            .map { psiClass ->
                psiClass?.qualifiedName?.let {
                    LookupElementBuilder.create(psiClass, it)
                        .withRenderer(ClassReferencePropertyRenderer())

                }
            }.filterNotNull().toList()
    }

    private fun getBeanReferences(targetClassFqn: String): List<Any> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyList()
        val springSearchService = SpringSearchServiceFacade.getInstance(module.project)
        val foundActiveBeans = springSearchService.getAllActiveBeans(module)
        return foundActiveBeans.asSequence()
            .filter { targetClassFqn.isBlank() || it.psiClass.qualifiedName == targetClassFqn }
            .map {
                LookupElementBuilder.create(it.name)
                    .withIcon(AllIcons.Nodes.Class)
                    .withTailText(" (${it.psiMember.containingFile?.name})")
                    .withTypeText(it.psiClass.name)
            }.toList()
    }

    private fun isMapWithoutValue(propertyKey: String, propertyHint: PropertyHint?): Boolean {
        if (propertyHint == null) {
            return false
        }
        if (propertyHint.name.substringAfterLast(".") == VALUES
            && propertyHint.name.substringBeforeLast(".") == propertyKey
        ) {
            return true
        }
        return false
    }
}