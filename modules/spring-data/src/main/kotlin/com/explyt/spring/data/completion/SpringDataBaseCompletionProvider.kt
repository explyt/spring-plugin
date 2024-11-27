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

package com.explyt.spring.data.completion

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.data.util.RepositoryTypes
import com.explyt.spring.data.util.SpringDataRepositoryUtil
import com.explyt.spring.data.util.SpringDataUtil
import com.explyt.util.ExplytPsiUtil.returnPsiType
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ObjectUtils
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.*
import org.springframework.data.repository.query.parser.PartTree
import kotlin.math.max

class SpringDataBaseCompletionProvider : CompletionProvider<CompletionParameters>() {
    private val existPattern = PartTree.EXISTS_PATTERN.split("|")
    private val countPattern = PartTree.COUNT_PATTERN.split("|")
    private val removePattern = PartTree.DELETE_PATTERN.split("|")
    private val queryPattern = PartTree.QUERY_PATTERN.split("|")

    private val distinctAllSuffix = listOf("Distinct", "All")
    private val firstTopSuffix = listOf("First", "Top", "DistinctFirst", "DistinctTop")
    private val sortOrder = listOf("Desc", "Asc")
    private val orderBy = "OrderBy"
    private val allOperators = listOf(
        "Distinct", "And", "Or", "Is", "Equals",
        "Between", "LessThan", "LessThanEqual", "GreaterThan", "GreaterThanEqual", "After",
        "Before", "IsNull", "Null", "IsNotNull", "NotNull", "Like",
        "NotLike", "StartingWith", "EndingWith", "Containing", "OrderBy", "Not",
        "In", "NotIn", "True", "False", "IgnoreCase"
    )

    override fun addCompletions(
        parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet
    ) {
        val repositoryTypes = getRepositoryTypes(parameters) ?: return
        val positionContext = getCurrentPositionContext(parameters) ?: return
        result.addAllElements(getCompletionVariants(repositoryTypes, positionContext, result.prefixMatcher))
        StatisticService.getInstance().addActionUsage(StatisticActionId.COMPLETION_SPRING_DATA_METHOD)
        result.stopHere()
    }

    private fun getCompletionVariants(
        repositoryTypes: RepositoryTypes, positionContext: PositionContext, matcher: PrefixMatcher
    ): Set<LookupElement> {
        val methodName = positionContext.methodName
        val methodPrefix = matcher.prefix
        val domainClass = repositoryTypes.psiClass
        val offset = methodPrefix.length

        val subjectVariants = getSubjectVariants(domainClass, positionContext)
        val domainProperties = SpringDataUtil.getProperties(domainClass).map { StringUtil.capitalize(it) }

        //start of input: <caret>/fin<caret>/r<caret>/remov<caret> and etc
        if (subjectVariants.contains(methodPrefix)) {
            return getCompletionVariants(domainProperties.toSet(), methodPrefix, "")
        } else if (StringUtil.isEmptyOrSpaces(methodPrefix) || subjectVariants.any { it.startsWith(methodPrefix) }) {
            return getCompletionVariants(subjectVariants, "", "")
        }

        val endWithProperty = domainProperties.find { methodPrefix.endsWith(it) }
        val suffix = getSuffix(methodName, offset)
        if (endWithProperty != null) {
            //case when caret after property: findById<Caret>/findByName<caret>OrAddress
            return getCompletionVariants(getOperators(methodPrefix).toSet(), methodPrefix, suffix)
        } else {
            val prefix = getPrefix(methodName, offset)
            val lookupStrings = (domainProperties + getOperators(methodPrefix)).toSet()
            return getLookupElements(lookupStrings, prefix, suffix, methodPrefix)
        }
    }

    private fun SpringDataBaseCompletionProvider.getLookupElements(
        lookupStrings: Set<String>, prefix: String, suffix: String, methodPrefix: String
    ): Set<LookupElement> {
        if (methodPrefix == prefix) return getCompletionVariants(lookupStrings, prefix, suffix)
        return getCompletionVariants(lookupStrings, prefix, suffix) +
                getCompletionVariants(lookupStrings, methodPrefix, suffix)
    }

    private fun getRepositoryTypes(parameters: CompletionParameters): RepositoryTypes? {
        val aClass: UClass = parameters.position.findContaining(UClass::class.java) ?: return null
        return SpringDataRepositoryUtil.getGenericTypes(aClass.javaPsi)
    }

    private fun getCurrentPositionContext(parameters: CompletionParameters): PositionContext? {
        var declaration = parameters.position.findContaining(UDeclaration::class.java)
        if (declaration is UParameter) {
            declaration = ObjectUtils.tryCast(declaration.uastParent, UDeclaration::class.java)
        }
        if (declaration == null || declaration.uastParent !is UClass) return null
        val sourceElement = declaration.sourcePsi ?: return null

        val originalElement = getOriginalElement(sourceElement, PsiElement::class.java)
        val psiType = psiTypeElement(originalElement)
        val originalUDeclaration = originalElement.findContaining(UDeclaration::class.java)
        return (originalUDeclaration as? UAnchorOwner)?.uastAnchor?.name?.let { PositionContext(it, psiType) }
    }

    private fun psiTypeElement(originalElement: PsiElement?): PsiType? {
        if (originalElement is PsiTypeElement) {
            return originalElement.type
        }
        return (originalElement.toUElement() as? UField)?.returnPsiType
    }

    private fun getSubjectVariants(
        domainClassName: PsiClass, positionContext: PositionContext? = null
    ): Set<String> {
        val strings = HashSet<String>()
        if (SpringDataRepositoryUtil.isVoidType(positionContext?.psiType)) {
            removePattern.forEach { addSubjectSimpleTails(strings, it, domainClassName) }
        } else if (SpringDataRepositoryUtil.isBooleanType(positionContext?.psiType)) {
            existPattern.forEach { addSubjectSimpleTails(strings, it, domainClassName) }
        } else if (SpringDataRepositoryUtil.isNumberType(positionContext?.psiType)) {
            countPattern.forEach { addSubjectSimpleTails(strings, it, domainClassName) }
            removePattern.forEach { addSubjectSimpleTails(strings, it, domainClassName) }
        } else if (positionContext?.psiType == null) {
            addQueryPattern(strings, domainClassName)
            removePattern.forEach { addSubjectSimpleTails(strings, it, domainClassName) }
            existPattern.forEach { addSubjectSimpleTails(strings, it, domainClassName) }
            countPattern.forEach { addSubjectSimpleTails(strings, it, domainClassName) }
        } else {
            addQueryPattern(strings, domainClassName)
        }
        val result = mutableSetOf<String>()
        for (prefixValue in strings) {
            result.add(prefixValue + "By")
            if (prefixValue.endsWith("All")) result.add(prefixValue)
        }
        return result
    }

    private fun addQueryPattern(strings: HashSet<String>, domainClassName: PsiClass) {
        for (s in queryPattern) {
            addSubjectSimpleTails(strings, s, domainClassName)
            addSimpleTails(strings, s)
        }
    }

    private fun addSubjectSimpleTails(strings: MutableCollection<String>, base: String, domainClass: PsiClass) {
        addTails(strings, base, distinctAllSuffix)
        val domainClassName = domainClass.name
        if (domainClassName != null) {
            addTails(strings, base, listOf(domainClassName, StringUtil.pluralize(domainClassName)))
        }
    }

    private fun addSimpleTails(strings: MutableCollection<String>, s: String) {
        addTails(strings, s, firstTopSuffix)
    }

    private fun addTails(strings: MutableCollection<String>, base: String, tails: Collection<String>) {
        strings.add(base)
        for (tail in tails) {
            strings.add(base + tail)
        }
    }

    private fun getSuffix(methodName: String, offset: Int): String {
        val charArray = methodName.toCharArray()
        for (i in charArray.indices) {
            if (i >= offset && Character.isUpperCase(charArray[i])) {
                return methodName.substring(i)
            }
        }
        return ""
    }

    private fun getPrefix(methodName: String, offset: Int): String {
        val charArray = methodName.toCharArray()
        var maxUpperCasePosition = 0
        for (i in charArray.indices) {
            if (i <= offset && Character.isUpperCase(charArray[i])) {
                maxUpperCasePosition = max(i, maxUpperCasePosition)
            }
        }
        return if (maxUpperCasePosition > 0) methodName.substring(0, maxUpperCasePosition) else ""
    }

    private fun getCompletionVariants(
        strings: Set<String>, prefixExpression: String, suffixExpression: String?
    ): Set<LookupElement> {
        val set = HashSet<LookupElement>()
        for (s in strings) {
            if (StringUtil.isEmptyOrSpaces(s)) continue
            set.add(createLookupElement(prefixExpression, s, ""))
            if (!StringUtil.isEmptyOrSpaces(suffixExpression)) {
                set.add(createLookupElement(prefixExpression, s, suffixExpression!!))
            }
        }
        return set
    }

    private fun createLookupElement(
        prefixExpression: String, property: String, suffixExpression: String
    ): LookupElementBuilder {
        val lookupText = prefixExpression + property + suffixExpression
        return LookupElementBuilder.create(lookupText).withPresentableText(property).bold()
            .withTailText(if (StringUtil.isEmptyOrSpaces(suffixExpression)) "" else suffixExpression, true)
            .withIcon(SpringIcons.Spring).withInsertHandler(object : InsertHandler<LookupElement> {
                override fun handleInsert(context: InsertionContext, item: LookupElement) {
                    AutoPopupController.getInstance(context.project).scheduleAutoPopup(context.editor)
                }
            })
    }

    private fun <T : PsiElement?> getOriginalElement(psiElement: T, elementClass: Class<out T>): T? {
        if (psiElement == null) return null
        val psiFile = psiElement.containingFile
        val originalFile = psiFile.originalFile
        if (originalFile === psiFile) return psiElement
        val range = psiElement.textRange
        val element = originalFile.findElementAt(range.startOffset)
        val maxLength = range.length
        var parent = PsiTreeUtil.getParentOfType(element, elementClass, false)
        var next = parent
        while (next != null && next.textLength <= maxLength) {
            parent = next
            next = PsiTreeUtil.getParentOfType(next, elementClass, true)
        }
        return parent
    }

    private fun getOperators(methodPrefix: String): List<String> {
        return if (methodPrefix.contains(orderBy)) sortOrder else allOperators
    }
}

private data class PositionContext(val methodName: String, val psiType: PsiType?)