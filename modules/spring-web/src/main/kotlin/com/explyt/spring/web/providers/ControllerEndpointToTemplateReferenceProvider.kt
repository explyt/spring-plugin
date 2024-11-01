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

package com.explyt.spring.web.providers

import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.service.SpringSearchUtils
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.references.RestTemplateReferenceSet
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.childrenOfType
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.*

class ControllerEndpointToTemplateReferenceProvider : UastInjectionHostReferenceProvider() {

    override fun getReferencesForInjectionHost(
        uExpression: UExpression, host: PsiLanguageInjectionHost, context: ProcessingContext
    ): Array<PsiReference> {
        val psiElement = uExpression.sourcePsi ?: return emptyArray()
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return emptyArray()
        if (uExpression !is ULiteralExpression && uExpression !is UPolyadicExpression) return emptyArray()
        val uReturnExpression = uExpression.uastParent as? UReturnExpression ?: return emptyArray()
        val uMethod = uReturnExpression.getContainingUMethod() ?: return emptyArray()
        if (!uMethod.javaPsi.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)) return emptyArray()
        if (uMethod.javaPsi.isMetaAnnotatedBy(SpringWebClasses.RESPONSE_BODY)) return emptyArray()
        val psiClass = uMethod.getContainingUClass()?.javaPsi ?: return emptyArray()
        if (!psiClass.isMetaAnnotatedBy(SpringWebClasses.CONTROLLER)) return emptyArray()
        if (psiClass.isMetaAnnotatedBy(SpringWebClasses.RESPONSE_BODY)) return emptyArray()

        val path = uExpression.evaluateString() ?: return emptyArray()
        val range = ElementManipulators.getValueTextRange(psiElement)

        val templateResolverMethods = getTemplateResolverMethods(module)
        val prefix = templateResolverMethods["setPrefix"]?.let { findPathInUsage(it) }
            ?: getPropertyValue("spring.mvc.view.prefix", module)
            ?: getPropertyValue("spring.thymeleaf.prefix", module)
            ?: "classpath:/templates"
        val suffix = templateResolverMethods["setSuffix"]?.let { findPathInUsage(it) }
            ?: getPropertyValue("spring.mvc.view.suffix", module)
            ?: getPropertyValue("spring.thymeleaf.suffix", module)
            ?: ".html"

        val fileRefSet = RestTemplateReferenceSet(
            path, psiElement, range.startOffset,
            prefix,
            listOfNotNull(
                FileTypeRegistry.getInstance().getFileTypeByExtension(suffix.split(".").last())
            ).toTypedArray()
        )
        val toList = fileRefSet.allReferences
            .mapTo(mutableListOf()) { FileRefWithExt(fileRefSet, it.rangeInElement, it.index, it.text, prefix, suffix) }

        if (toList.isNotEmpty()) {
            val last = toList.last()
            toList.add(FileRefWithExt(fileRefSet, last.rangeInElement, last.index, last.text + suffix, prefix, suffix))
        }

        return toList.toTypedArray()

    }

    private fun getPropertyValue(propertyKey: String, module: Module): String? {
        return DefinedConfigurationPropertiesSearch.getInstance(module.project)
            .findProperties(module, propertyKey)
            .firstOrNull { it.value?.isNotBlank() == true }
            ?.value
    }

    private fun findPathInUsage(psiMethod: PsiMethod): String? {
        return SpringSearchUtils.getAllReferencesToElement(psiMethod).asSequence()
            .filterIsInstance<PsiReferenceExpression>()
            .map { it.element }
            .mapNotNull { it.context as? PsiMethodCallExpression }
            .mapNotNull {
                (it.childrenOfType<PsiExpressionList>().firstOrNull()
                    ?.expressions
                    ?.firstOrNull() as? PsiLiteralExpression)?.value as? String
            }
            .firstOrNull()
    }

    private fun getTemplateResolverMethods(module: Module): Map<String, PsiMethod> {
        return CachedValuesManager.getManager(module.project)
            .getCachedValue(module) {
                CachedValueProvider.Result(
                    doGetTemplateResolverMethods(module),
                    ModificationTrackerManager.getInstance(module.project).getLibraryTracker()
                )
            }
    }

    private fun doGetTemplateResolverMethods(module: Module): Map<String, PsiMethod> {
        val methods = SpringCoreUtil.getClassMethodsFromLibraries(SpringWebClasses.ABSTRACT_CONFIGURABLE_TEMPLATE_RESOLVER, module)
            ?: SpringCoreUtil.getClassMethodsFromLibraries(SpringWebClasses.URL_BASED_VIEW_RESOLVER, module)
            ?: emptyArray()

        return methods.associateBy { it.name }
    }

    private class FileRefWithExt(
        fileReferenceSet: FileReferenceSet,
        textRange: TextRange,
        index: Int,
        text: String,
        private val prefix: String,
        private val suffix: String
    ) :
        FileReference(fileReferenceSet, textRange, index, text) {

        override fun isReferenceTo(element: PsiElement): Boolean {
            return (element as? PsiFileSystemItem)?.name == text
        }

        override fun getVariants(): Array<Any> {
            val variants = super.getVariants()
                .map {
                    (it as? LookupElement)?.let {
                        val psiElement = it.psiElement ?: return@map it
                        LookupElementBuilder.create(psiElement, it.lookupString.split('.').first())
                            .withIcon((psiElement as? PsiFile)?.getIcon(0))
                    } ?: it
                }
                .toTypedArray()
            return variants
        }

        override fun rename(newName: String?): PsiElement {
            if (newName == null) return super.rename(null)

            val dir = prefix.removePrefix("classpath:")
            return super.rename(
                newName
                    .substringAfter(dir)
                    .removePrefix("/")
                    .removeSuffix(suffix)
            )
        }

    }

}