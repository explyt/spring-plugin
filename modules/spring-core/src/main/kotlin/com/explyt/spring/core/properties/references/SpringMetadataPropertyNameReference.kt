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

import com.explyt.spring.core.SpringProperties.CHAR_COLON
import com.explyt.spring.core.SpringProperties.CHAR_END_BRACKET
import com.explyt.spring.core.SpringProperties.CHAR_END_SQUARE_BRACKET
import com.explyt.spring.core.SpringProperties.CHAR_QUOTES
import com.explyt.spring.core.SpringProperties.CHAR_START_BRACKET
import com.explyt.spring.core.SpringProperties.CHAR_START_SQUARE_BRACKET
import com.explyt.spring.core.SpringProperties.NAME
import com.explyt.spring.core.properties.contributors.ContextCategory
import com.explyt.spring.core.properties.contributors.ItemVariant
import com.explyt.spring.core.properties.contributors.TypeVariant
import com.explyt.spring.core.properties.providers.SpringMetadataValueProvider
import com.explyt.spring.core.properties.references.SpringMetadataPropertyNameReference.Companion.propertyNameTail
import com.intellij.codeInsight.TailType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.TailTypeDecorator
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil
import java.util.*

class SpringMetadataPropertyNameReference(element: PsiElement) : PsiReferenceBase<PsiElement>(element) {

    companion object {
        val propertyNameTail = QuotesColonTailType()

        private val literalTail = StringLiteralTailType()
        private val arrayTail = ArrayTailType()
        private val objectTail = ObjectTailType()

        private fun getTailTypeForVariant(variantType: TypeVariant) = when (variantType) {
            TypeVariant.TEXT -> literalTail
            TypeVariant.ARRAY -> arrayTail
            TypeVariant.OBJECT -> objectTail
            TypeVariant.DEFAULT -> propertyNameTail
        }

        private val variantToLookupElementMapper: (ItemVariant) -> LookupElement = { variant ->
            val builder = LookupElementBuilder.create(variant.item)
            TailTypeDecorator.withTail(builder, getTailTypeForVariant(variant.variantType))
        }
    }

    private val localContextCategory: ContextCategory? = null

    override fun resolve(): PsiElement {
        return element
    }

    private val categoryVariantsMap = mapOf(
        ContextCategory.ROOT to arrayOf(ItemVariant.GROUPS, ItemVariant.PROPERTIES, ItemVariant.HINTS),
        ContextCategory.GROUPS to arrayOf(
            ItemVariant.TYPE,
            ItemVariant.SOURCE_TYPE,
            ItemVariant.NAME,
            ItemVariant.DESCRIPTION,
            ItemVariant.SOURCE_METHOD
        ),
        ContextCategory.PROPERTIES to arrayOf(
            ItemVariant.TYPE,
            ItemVariant.SOURCE_TYPE,
            ItemVariant.NAME,
            ItemVariant.DESCRIPTION,
            ItemVariant.DEFAULT_VALUE,
            ItemVariant.DEPRECATION
        ),
        ContextCategory.DEPRECATION to arrayOf(ItemVariant.REASON, ItemVariant.LEVEL, ItemVariant.REPLACEMENT),
        ContextCategory.HINTS to arrayOf(ItemVariant.NAME, ItemVariant.VALUES, ItemVariant.PROVIDERS),
        ContextCategory.HINTS_VALUES to arrayOf(ItemVariant.VALUE, ItemVariant.DESCRIPTION),
        ContextCategory.HINTS_PROVIDERS to arrayOf(ItemVariant.NAME, ItemVariant.PARAMETERS)
    )

    override fun getVariants(): Array<LookupElement> {
        return categoryVariantsMap[localContextCategory]?.let { createVariants(*it) }
            ?: when (localContextCategory) {
                ContextCategory.HINTS_PARAMETERS -> getParameterVariants()
                else -> emptyArray()
            }
    }

    private fun getParameterVariants(): Array<LookupElement> {
        val valueProvider = findValueProvider() ?: return emptyArray()
        val myVariantsFromParameters = EnumSet.noneOf(ItemVariant::class.java)
        for (parameter in valueProvider.parameters) {
            val variant = ItemVariant.findByName(parameter.name)
            if (variant != null) {
                myVariantsFromParameters.add(variant)
            }
        }

        return createVariants(myVariantsFromParameters)
    }

    private fun createVariants(vararg variants: ItemVariant): Array<LookupElement> {
        return createVariants(variants.toSet())
    }

    private fun createVariants(variants: Set<ItemVariant>): Array<LookupElement> {
        val jsonObject = PsiTreeUtil.getParentOfType(element, JsonObject::class.java) ?: return emptyArray()
        val existingProperties = jsonObject.propertyList.mapTo(HashSet()) { it.name }
        val filteredVariants = variants.filter { it.item !in existingProperties }
        return filteredVariants.map { variantToLookupElementMapper(it) }.toTypedArray()
    }

    private fun findValueProvider(): SpringMetadataValueProvider? {
        val parametersObject = PsiTreeUtil.getParentOfType(this.element, JsonObject::class.java) ?: return null
        val superParent = PsiTreeUtil.getParentOfType(parametersObject, JsonObject::class.java) ?: return null

        val nameLiteral = superParent.findProperty(NAME)?.value as? JsonStringLiteral ?: return null
        return nameLiteral.references
            .filterIsInstance<SpringMetadataValueProviderReference>()
            .firstOrNull()?.getValueProvider()
    }

}

class QuotesColonTailType : TailType() {
    override fun processTail(editor: Editor?, tailOffset: Int): Int {
        var localTailOffset = tailOffset
        localTailOffset = insertChar(editor, localTailOffset, CHAR_QUOTES)
        localTailOffset = insertChar(editor, localTailOffset, CHAR_COLON)
        return insertChar(editor, localTailOffset, ' ', false)
    }
}

class StringLiteralTailType : TailType() {
    override fun processTail(editor: Editor?, tailOffset: Int): Int {
        var localTailOffset = tailOffset
        localTailOffset = propertyNameTail.processTail(editor, localTailOffset)
        localTailOffset = insertChar(editor, localTailOffset, CHAR_QUOTES)
        localTailOffset = insertChar(editor, localTailOffset, CHAR_QUOTES)
        return moveCaret(editor, localTailOffset, -1)
    }
}

class ArrayTailType : TailType() {
    override fun processTail(editor: Editor, tailOffset: Int): Int {
        var localTailOffset = tailOffset
        localTailOffset = propertyNameTail.processTail(editor, localTailOffset)
        localTailOffset = insertChar(editor, localTailOffset, CHAR_START_SQUARE_BRACKET)
        localTailOffset = insertChar(editor, localTailOffset, CHAR_START_BRACKET)
        localTailOffset = insertChar(editor, localTailOffset, CHAR_END_BRACKET)
        localTailOffset = insertChar(editor, localTailOffset, CHAR_END_SQUARE_BRACKET)
        return moveCaret(editor, localTailOffset, -2)
    }
}

class ObjectTailType : TailType() {
    override fun processTail(editor: Editor?, tailOffset: Int): Int {
        var localTailOffset = tailOffset
        localTailOffset = propertyNameTail.processTail(editor, localTailOffset)
        localTailOffset = insertChar(editor, localTailOffset, CHAR_START_BRACKET)
        localTailOffset = insertChar(editor, localTailOffset, CHAR_END_BRACKET)
        return moveCaret(editor, localTailOffset, -1)
    }
}