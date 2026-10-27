/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.references

import com.explyt.spring.core.SpringProperties.LOGGING_LEVEL
import com.explyt.spring.core.SpringProperties.POSTFIX_KEYS
import com.explyt.spring.core.SpringProperties.VALUE
import com.explyt.spring.core.SpringProperties.VALUES
import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.completion.properties.MetadataDeclarations
import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.completion.properties.ValueHint
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticInsertHandler
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.PropertyUtil.DOT
import com.explyt.util.ExplytTextUtil.getFirstSentenceWithoutDot
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet

/**
 * The suffix of `logging.level.<suffix>` names either a logging group or a logger name, and Spring says which:
 * the `logging.level.keys` hint declares the groups the framework ships, `logging.group.<name>` declares the ones a
 * project adds, and anything else is a logger name, that is a package or a class.
 */
object LoggingLevelKeys {

    private const val LOGGING_GROUP = "logging.group"

    private const val KEYS_HINT_NAME = LOGGING_LEVEL + POSTFIX_KEYS

    private val loggerNameProvider = JavaClassReferenceProvider().apply {
        // A logger name that names nothing is legal - the logger simply has no output - so an unresolved segment must
        // not be reported as an error.
        isSoft = true
        // Without advanced resolve the last segment of the chain is only looked up as a class, so `springframework` of
        // `org.springframework` resolved to nothing while the `org` before it resolved to its package.
        setOption(JavaClassReferenceProvider.ADVANCED_RESOLVE, true)
    }

    /**
     * The references covering [suffix], which occupies [range] of [element].
     *
     * [offerGroupVariants] belongs to the caller because the two configuration formats complete the suffix
     * differently: in `.properties` the group names come from this reference, while in YAML the completion
     * contributor already offers them, filtered by the groups the file has not used yet.
     */
    fun referencesForSuffix(
        element: PsiElement,
        module: Module,
        suffix: String,
        range: TextRange,
        offerGroupVariants: Boolean = false
    ): Array<PsiReference> {
        // A group is a single name, so a suffix that is a chain of segments can only be a logger name. Skipping the
        // group reference there also keeps it from covering a range it can never resolve.
        if (suffix.contains(DOT)) return loggerNameReferences(element, suffix, range)

        val groupReference = LoggingLevelGroupReference(element, module, range, suffix, offerGroupVariants)
        // A group and a logger name are mutually exclusive, so a declared group is not offered the logger-name chain as
        // a competing target for the same range.
        if (isGroup(module, suffix)) return arrayOf(groupReference)

        return arrayOf(groupReference, *loggerNameReferences(element, suffix, range))
    }

    private fun loggerNameReferences(element: PsiElement, suffix: String, range: TextRange): Array<PsiReference> =
        JavaClassReferenceSet(suffix, element, range.startOffset, false, loggerNameProvider).references

    /** The groups Spring itself declares, as literals of the `logging.level.keys` hint. */
    internal fun declaredGroupHints(module: Module): List<ValueHint> {
        return SpringConfigurationPropertiesSearch.getInstance(module.project)
            .findHint(module, KEYS_HINT_NAME)
            ?.values.orEmpty()
    }

    private fun isGroup(module: Module, suffix: String): Boolean {
        if (suffix.isEmpty()) return false
        if (declaredGroupHints(module).any { it.value.equals(suffix, ignoreCase = true) }) return true
        return DefinedConfigurationPropertiesSearch.getInstance(module.project)
            .getPropertiesCommonKeyMap(module)
            .containsKey(PropertyUtil.toCommonPropertyForm("$LOGGING_GROUP.$suffix"))
    }

    /** The `"value": "<group>"` property of the `logging.level.keys` hint, or `null` when Spring declares no such group. */
    internal fun declaredGroupDeclaration(module: Module, group: String): JsonProperty? {
        val declarations = SpringConfigurationPropertiesSearch.getInstance(module.project)
            .getElementNameHints(module)
            .filter { it.name == KEYS_HINT_NAME }
            .mapNotNull { valueDeclaration(it.jsonProperty, group) }
        // Both metadata files of an artifact and its sources jar ship the same hint; they are one declaration.
        return MetadataDeclarations.preferred(declarations) { it.containingFile }
    }

    /** The `logging.group.<group>` entries of the project's own configuration files. */
    internal fun definedGroupDeclarations(module: Module, group: String): List<PsiElement> {
        return DefinedConfigurationPropertiesSearch.getInstance(module.project)
            .getPropertiesCommonKeyMap(module)[PropertyUtil.toCommonPropertyForm("$LOGGING_GROUP.$group")]
            .orEmpty()
            .mapNotNull { it.psiElement }
    }

    private fun valueDeclaration(hintName: JsonProperty, group: String): JsonProperty? {
        val values = (hintName.parent as? JsonObject)?.findProperty(VALUES)?.value as? JsonArray ?: return null
        val matching = values.valueList.asSequence()
            .mapNotNull { it as? JsonObject }
            .mapNotNull { it.findProperty(VALUE) }
            .filter { (it.value as? JsonStringLiteral)?.value.equals(group, ignoreCase = true) }
            .toList()
        // An exact match wins, in case a hint ever ships two literals differing by case alone.
        return matching.firstOrNull { (it.value as? JsonStringLiteral)?.value == group } ?: matching.firstOrNull()
    }

    internal fun groupLookupElement(valueHint: ValueHint): LookupElementBuilder? {
        val value = valueHint.value ?: return null
        val tailText = valueHint.description?.let { " (${getFirstSentenceWithoutDot(it)})" } ?: ""
        return LookupElementBuilder.create(value)
            .appendTailText(tailText, true)
            .withIcon(AllIcons.Nodes.Property)
            .withInsertHandler(StatisticInsertHandler(StatisticActionId.COMPLETION_PROPERTY_KEY_CONFIGURATION))
    }
}

/**
 * The suffix of `logging.level.<suffix>` read as a logging group: a value of the `logging.level.keys` hint, or a
 * `logging.group.<suffix>` entry of the project's own configuration.
 */
class LoggingLevelGroupReference(
    element: PsiElement,
    private val module: Module,
    rangeInElement: TextRange,
    private val group: String,
    private val offerVariants: Boolean
) : PsiReferenceBase.Poly<PsiElement>(element, rangeInElement, true) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (group.isEmpty()) return ResolveResult.EMPTY_ARRAY
        LoggingLevelKeys.declaredGroupDeclaration(module, group)
            ?.let { return PsiElementResolveResult.createResults(it) }
        return PsiElementResolveResult.createResults(LoggingLevelKeys.definedGroupDeclarations(module, group))
    }

    override fun getVariants(): Array<Any> {
        if (!offerVariants) return emptyArray()
        return LoggingLevelKeys.declaredGroupHints(module)
            .mapNotNull { LoggingLevelKeys.groupLookupElement(it) }
            .toTypedArray()
    }
}
