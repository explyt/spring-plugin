package com.esprito.spring.core.properties.references

import com.esprito.spring.core.SpringProperties.CHAR_COLON
import com.esprito.spring.core.SpringProperties.CHAR_END_BRACKET
import com.esprito.spring.core.SpringProperties.CHAR_END_SQUARE_BRACKET
import com.esprito.spring.core.SpringProperties.CHAR_QUOTES
import com.esprito.spring.core.SpringProperties.CHAR_START_BRACKET
import com.esprito.spring.core.SpringProperties.CHAR_START_SQUARE_BRACKET
import com.esprito.spring.core.SpringProperties.NAME
import com.esprito.spring.core.properties.providers.SpringBootValueProvider
import com.intellij.codeInsight.TailType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.TailTypeDecorator
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Function
import com.intellij.util.ObjectUtils
import com.intellij.util.containers.ContainerUtil
import java.util.*

class AdditionalConfigPropertyNameReference(element: PsiElement) : PsiReferenceBase<PsiElement>(element) {

    companion object {
        private val CLOSE_PROPERTY_NAME_TAIL = object : TailType() {
            override fun processTail(editor: Editor?, tailOffset: Int): Int {
                var localTailOffset = tailOffset
                localTailOffset = insertChar(editor, localTailOffset, CHAR_QUOTES)
                localTailOffset = insertChar(editor, localTailOffset, CHAR_COLON)

                return insertChar(editor, localTailOffset, ' ', false)
            }
        }

        private val STRING_LITERAL_TAIL = object : TailType() {
            override fun processTail(editor: Editor?, tailOffset: Int): Int {
                var localTailOffset = tailOffset
                localTailOffset = CLOSE_PROPERTY_NAME_TAIL.processTail(editor, localTailOffset)
                localTailOffset = insertChar(editor, localTailOffset, CHAR_QUOTES)
                localTailOffset = insertChar(editor, localTailOffset, CHAR_QUOTES)
                return moveCaret(editor, localTailOffset, -1)
            }
        }

        private val ARRAY_TAIL = object : TailType() {
            override fun processTail(editor: Editor?, tailOffset: Int): Int {
                var localTailOffset = tailOffset
                localTailOffset = CLOSE_PROPERTY_NAME_TAIL.processTail(editor, localTailOffset)
                localTailOffset = insertChar(editor, localTailOffset, CHAR_START_SQUARE_BRACKET)
                localTailOffset = insertChar(editor, localTailOffset, CHAR_START_BRACKET)
                localTailOffset = insertChar(editor, localTailOffset, CHAR_END_BRACKET)
                localTailOffset = insertChar(editor, localTailOffset, CHAR_END_SQUARE_BRACKET)
                return moveCaret(editor, localTailOffset, -2)
            }
        }

        private val OBJECT_TAIL = object : TailType() {
            override fun processTail(editor: Editor?, tailOffset: Int): Int {
                var localTailOffset = tailOffset
                localTailOffset = CLOSE_PROPERTY_NAME_TAIL.processTail(editor, localTailOffset)
                localTailOffset = insertChar(editor, localTailOffset, CHAR_START_BRACKET)
                localTailOffset = insertChar(editor, localTailOffset, CHAR_END_BRACKET)
                return moveCaret(editor, localTailOffset, -1)
            }
        }

        private val VARIANT_LOOKUP_ELEMENT_FUNCTION: Function<Variant, LookupElement> =
            Function<Variant, LookupElement> { variant: Variant ->
                val builder: LookupElementBuilder =
                    LookupElementBuilder.create(variant.nameVariant)
                val tailType = when (variant.variantType) {
                    VariantType.STRING_LITERAL -> STRING_LITERAL_TAIL
                    VariantType.ARRAY -> ARRAY_TAIL
                    VariantType.OBJECT -> OBJECT_TAIL
                    VariantType.DEFAULT -> CLOSE_PROPERTY_NAME_TAIL
                }
                TailTypeDecorator.withTail(builder, tailType)
            }

    }

    private val myGroupContext: GroupContext? = null

    override fun resolve(): PsiElement {
        return element
    }

    override fun getVariants(): Array<LookupElement> {
        return when (this.myGroupContext) {
            GroupContext.FAKE_TOP_LEVEL -> createVariants(
                EnumSet.of(
                    Variant.TOP_LEVEL_GROUPS,
                    Variant.TOP_LEVEL_PROPERTIES,
                    Variant.TOP_LEVEL_HINTS
                )
            )

            GroupContext.GROUPS -> createVariants(
                EnumSet.of(
                    Variant.TYPE,
                    Variant.SOURCE_TYPE,
                    Variant.NAME,
                    Variant.DESCRIPTION,
                    Variant.SOURCE_METHOD
                )
            )

            GroupContext.PROPERTIES -> createVariants(
                EnumSet.of(
                    Variant.TYPE,
                    Variant.SOURCE_TYPE,
                    Variant.NAME,
                    Variant.DESCRIPTION,
                    Variant.DEFAULT_VALUE,
                    Variant.DEPRECATION
                )
            )

            GroupContext.DEPRECATION -> createVariants(
                EnumSet.of(
                    Variant.REASON,
                    Variant.LEVEL,
                    Variant.REPLACEMENT
                )
            )

            GroupContext.HINTS -> createVariants(
                EnumSet.of(
                    Variant.NAME,
                    Variant.VALUES,
                    Variant.PROVIDERS
                )
            )

            GroupContext.HINTS_VALUES -> createVariants(
                EnumSet.of(
                    Variant.VALUE,
                    Variant.DESCRIPTION
                )
            )

            GroupContext.HINTS_PROVIDERS -> createVariants(
                EnumSet.of(
                    Variant.NAME,
                    Variant.PARAMETERS
                )
            )

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

    private fun createVariants(variants: EnumSet<Variant>): Array<LookupElement> {
        val jsonObject = PsiTreeUtil.getParentOfType(element, JsonObject::class.java)
        if (jsonObject == null) {
            return emptyArray()
        } else {
            val existingProperties = ContainerUtil.map2Set(jsonObject.propertyList) { obj: JsonProperty -> obj.name }
            val filteredVariants: EnumSet<Variant> = EnumSet.noneOf(Variant::class.java)
            for (variant in variants) {
                if (!existingProperties.contains(variant.nameVariant)) {
                    filteredVariants.add(variant)
                }
            }

            return ContainerUtil.map2Array(filteredVariants, LookupElement::class.java, VARIANT_LOOKUP_ELEMENT_FUNCTION)
        }
    }

    private fun findValueProvider(): SpringBootValueProvider? {
        val parametersObject = PsiTreeUtil.getParentOfType(this.element, JsonObject::class.java) ?: return null
        val superParent = PsiTreeUtil.getParentOfType(parametersObject, JsonObject::class.java) ?: return null

        val nameProperty = superParent.findProperty(NAME) ?: return null
        val nameLiteral = ObjectUtils.tryCast(nameProperty.value, JsonStringLiteral::class.java) ?: return null

        for (reference in nameLiteral.references) {
            if (reference is AdditionalConfigValueProviderReference) {
                return reference.getValueProvider()
            }
        }

        return null
    }

    enum class GroupContext(private val propertyName: String) {
        FAKE_TOP_LEVEL("topLevel"),
        GROUPS("groups"),
        PROPERTIES("properties"),
        DEPRECATION("deprecation"),
        HINTS("hints"),
        HINTS_VALUES("values"),
        HINTS_PROVIDERS("providers"),
        HINTS_PARAMETERS("parameters");

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
        TOP_LEVEL_GROUPS("groups", VariantType.ARRAY),
        TOP_LEVEL_PROPERTIES("properties", VariantType.ARRAY),
        TOP_LEVEL_HINTS("hints", VariantType.ARRAY),
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