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
import com.intellij.util.Function
import java.util.*

class SpringMetadataPropertyNameReference(element: PsiElement) : PsiReferenceBase<PsiElement>(element) {

    companion object {
        val propertyNameTail = QuotesColonTailType()

        private val literalTail = StringLiteralTailType()
        private val arrayTail = ArrayTailType()
        private val objectTail = ObjectTailType()

        private val variantToLookupElementMapper: Function<Variant, LookupElement> =
            Function<Variant, LookupElement> { variant: Variant ->
                val builder: LookupElementBuilder =
                    LookupElementBuilder.create(variant.nameVariant)
                val tailType = when (variant.variantType) {
                    VariantType.STRING_LITERAL -> literalTail
                    VariantType.ARRAY -> arrayTail
                    VariantType.OBJECT -> objectTail
                    VariantType.DEFAULT -> propertyNameTail
                }
                TailTypeDecorator.withTail(builder, tailType)
            }

    }

    private val myGroupContext: GroupContext? = null

    override fun resolve(): PsiElement {
        return element
    }

    override fun getVariants(): Array<LookupElement> {
        return when (myGroupContext) {
            GroupContext.TOP_LEVEL -> createVariants(Variant.GROUPS, Variant.PROPERTIES, Variant.HINTS)
            GroupContext.GROUPS -> createVariants(
                Variant.TYPE,
                Variant.SOURCE_TYPE,
                Variant.NAME,
                Variant.DESCRIPTION,
                Variant.SOURCE_METHOD
            )

            GroupContext.PROPERTIES -> createVariants(
                Variant.TYPE,
                Variant.SOURCE_TYPE,
                Variant.NAME,
                Variant.DESCRIPTION,
                Variant.DEFAULT_VALUE,
                Variant.DEPRECATION
            )

            GroupContext.DEPRECATION -> createVariants(Variant.REASON, Variant.LEVEL, Variant.REPLACEMENT)
            GroupContext.HINTS -> createVariants(Variant.NAME, Variant.VALUES, Variant.PROVIDERS)
            GroupContext.HINTS_VALUES -> createVariants(Variant.VALUE, Variant.DESCRIPTION)
            GroupContext.HINTS_PROVIDERS -> createVariants(Variant.NAME, Variant.PARAMETERS)
            GroupContext.HINTS_PARAMETERS -> getParameterVariants()
            else -> emptyArray()
        }
    }

    private fun getParameterVariants(): Array<LookupElement> {
        val valueProvider = findValueProvider() ?: return emptyArray()
        val myVariantsFromParameters = EnumSet.noneOf(Variant::class.java)
        for (parameter in valueProvider.parameters) {
            val variant = Variant.findByName(parameter.name)
            if (variant != null) {
                myVariantsFromParameters.add(variant)
            }
        }

        return createVariants(myVariantsFromParameters)
    }

    private fun createVariants(vararg variants: Variant): Array<LookupElement> {
        return createVariants(variants.toSet())
    }

    private fun createVariants(variants: Set<Variant>): Array<LookupElement> {
        val jsonObject = PsiTreeUtil.getParentOfType(element, JsonObject::class.java) ?: return emptyArray()
        val existingProperties = jsonObject.propertyList.mapTo(HashSet()) { it.name }
        val filteredVariants = variants.filter { it.nameVariant !in existingProperties }
        return filteredVariants.map { variantToLookupElementMapper.apply(it) }.toTypedArray()
    }

    private fun findValueProvider(): SpringMetadataValueProvider? {
        val parametersObject = PsiTreeUtil.getParentOfType(this.element, JsonObject::class.java) ?: return null
        val superParent = PsiTreeUtil.getParentOfType(parametersObject, JsonObject::class.java) ?: return null

        val nameLiteral = superParent.findProperty(NAME)?.value as? JsonStringLiteral ?: return null
        return nameLiteral.references
            .filterIsInstance<SpringMetadataValueProviderReference>()
            .firstOrNull()?.getValueProvider()
    }

    enum class GroupContext(private val propertyName: String) {
        TOP_LEVEL("topLevel"),
        HINTS("hints"),
        HINTS_VALUES("values"),
        HINTS_PROVIDERS("providers"),
        HINTS_PARAMETERS("parameters"),
        GROUPS("groups"),
        PROPERTIES("properties"),
        DEPRECATION("deprecation");

        companion object {
            fun forProperty(propertyName: String): GroupContext? {
                for (groupContext in entries.toTypedArray()) {
                    if (groupContext.propertyName == propertyName) {
                        return groupContext
                    }
                }
                return null
            }
        }
    }

    private enum class Variant(val nameVariant: String, val variantType: VariantType) {
        GROUPS("groups", VariantType.ARRAY),
        PROPERTIES("properties", VariantType.ARRAY),
        HINTS("hints", VariantType.ARRAY),
        NAME("name", VariantType.STRING_LITERAL),
        TYPE("type", VariantType.STRING_LITERAL),
        SOURCE_TYPE("sourceType", VariantType.STRING_LITERAL),
        SOURCE_METHOD("sourceMethod", VariantType.STRING_LITERAL),
        DESCRIPTION("description", VariantType.STRING_LITERAL),
        DEFAULT_VALUE("defaultValue", VariantType.DEFAULT),
        DEPRECATED("deprecated", VariantType.DEFAULT),
        DEPRECATION("deprecation", VariantType.OBJECT),
        REASON("reason", VariantType.STRING_LITERAL),
        REPLACEMENT("replacement", VariantType.STRING_LITERAL),
        LEVEL("level", VariantType.STRING_LITERAL),
        VALUES("values", VariantType.ARRAY),
        PROVIDERS("providers", VariantType.ARRAY),
        VALUE("value", VariantType.DEFAULT),
        PARAMETERS("parameters", VariantType.OBJECT),
        TARGET("target", VariantType.STRING_LITERAL),
        CONCRETE("concrete", VariantType.DEFAULT),
        GROUP("group", VariantType.DEFAULT);


        companion object {
            fun findByName(name: String): Variant? {
                for (variant in entries) {
                    if (variant.nameVariant == name) {
                        return variant
                    }
                }
                return null
            }
        }
    }

    private enum class VariantType {
        DEFAULT,
        STRING_LITERAL,
        ARRAY,
        OBJECT
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