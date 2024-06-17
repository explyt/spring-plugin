package com.esprito.spring.core.properties.contributors

import com.esprito.spring.core.SpringProperties.DEPRECATION
import com.esprito.spring.core.SpringProperties.NAME
import com.esprito.spring.core.SpringProperties.POSTFIX_KEYS
import com.esprito.spring.core.SpringProperties.POSTFIX_VALUES
import com.esprito.spring.core.SpringProperties.REPLACEMENT
import com.esprito.spring.core.SpringProperties.SOURCE_TYPE
import com.esprito.spring.core.SpringProperties.TARGET
import com.esprito.spring.core.SpringProperties.TYPE
import com.esprito.spring.core.properties.providers.ConfigurationPropertyKeyReference
import com.esprito.spring.core.properties.references.AdditionalConfigPropertyNameReference
import com.esprito.spring.core.properties.references.AdditionalConfigValueProviderReference
import com.esprito.spring.core.util.SpringCoreUtil
import com.intellij.json.psi.*
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiFilePattern
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet
import com.intellij.util.ProcessingContext

class AdditionalConfigReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {

        val originalClassReferenceProvider = JavaClassReferenceProvider()
        originalClassReferenceProvider.setOption(JavaClassReferenceProvider.ALLOW_DOLLAR_NAMES, java.lang.Boolean.TRUE)

        val javaClassReferenceProvider = getJavaClassReferenceProvider(originalClassReferenceProvider)

        // -- type or sourceType
        registrar.registerReferenceProvider(
            ((PlatformPatterns.psiElement(JsonStringLiteral::class.java)
                .inFile(StandardPatterns.or(additionalConfigMetadata, springConfigMetadata)))
                .with(propertyValue))
                .withParent(
                    PlatformPatterns.psiElement(JsonProperty::class.java)
                        .with(classPropertyNames)
                ),
            javaClassReferenceProvider
        )

        // -- hints.providers.parameters.target
        registrar.registerReferenceProvider(
            (((PlatformPatterns.psiElement(JsonStringLiteral::class.java)
                .inFile(StandardPatterns.or(additionalConfigMetadata, springConfigMetadata)))
                .with(propertyValue))
                .withParent(
                    PlatformPatterns.psiElement(JsonProperty::class.java)
                        .with(targetProperty)
                ))
                .withSuperParent(3, groupContextWithObject),
            javaClassReferenceProvider
        )

        // -- hints.name
        registrar.registerReferenceProvider(
            ((((PlatformPatterns.psiElement(JsonStringLiteral::class.java)
                .inFile(additionalConfigMetadata))
                .with(propertyValue))
                .withParent(
                    PlatformPatterns.psiElement(JsonProperty::class.java)
                        .with(propertyName)
                ))
                .withSuperParent(4, groupContextWithArray))
                .with(hintsGroup),
            getHintNameReferenceProvider()
        )

        // -- hints.providers
        registrar.registerReferenceProvider(
            ((((PlatformPatterns.psiElement(JsonStringLiteral::class.java)
                .inFile(additionalConfigMetadata))
                .with(propertyValue))
                .withParent(
                    PlatformPatterns.psiElement(JsonProperty::class.java)
                        .with(propertyName)
                ))
                .withSuperParent(4, groupContextWithArray))
                .with(hintsProvidersGroup),
            getHintProvidersReferenceProvider()
        )
    }

    private fun getHintNameReferenceProvider(): PsiReferenceProvider =
        object : PsiReferenceProvider() {
            override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
                val property = ElementManipulators.getValueText(element)
                val prefixLength = getPrefixLength(property)
                val textRange = if (prefixLength != -1) TextRange.from(1, prefixLength) else null
                val propertyKey = if (prefixLength != -1) property.substring(0, prefixLength) else property
                return arrayOf(ConfigurationPropertyKeyReference(element, module, propertyKey, textRange, "Hint"))
            }

            private fun getPrefixLength(property: String): Int {
                var prefixLength = -1
                if (property.endsWith(POSTFIX_KEYS) || property.endsWith(POSTFIX_VALUES)) {
                    prefixLength = property.lastIndexOf(POSTFIX_KEYS)
                    if (prefixLength == -1) {
                        prefixLength = property.lastIndexOf(POSTFIX_VALUES)
                    }
                }
                return prefixLength
            }
        }

    private fun getHintProvidersReferenceProvider(): PsiReferenceProvider =
        object : PsiReferenceProvider() {
            override fun getReferencesByElement(
                element: PsiElement,
                context: ProcessingContext
            ): Array<PsiReference> {
                return arrayOf(AdditionalConfigValueProviderReference(element))
            }
        }

    private fun getJavaClassReferenceProvider(originalProvider: JavaClassReferenceProvider): PsiReferenceProvider =
        object : PsiReferenceProvider() {
            override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                val javaClassReference = object : JavaClassReferenceSet(
                    ElementManipulators.getValueText(element),
                    element,
                    ElementManipulators.getOffsetInElement(element),
                    false,
                    originalProvider
                ) {
                    override fun isAllowDollarInNames(): Boolean {
                        return true
                    }
                }
                return javaClassReference.references
            }
        }

    companion object Holder {

        private val groupContextKey: Key<AdditionalConfigPropertyNameReference.GroupContext> =
            Key.create("groupContext")

        private val groupContext = object : PatternCondition<JsonProperty>("groupContext") {
            override fun accepts(jsonProperty: JsonProperty, context: ProcessingContext): Boolean {
                val propertyName = jsonProperty.name
                val groupContext =
                    AdditionalConfigPropertyNameReference.GroupContext.forProperty(propertyName) ?: return false
                context.put(groupContextKey, groupContext)
                return true
            }
        }
        private val groupContextWithObject = (PlatformPatterns.psiElement(JsonProperty::class.java)
            .with(groupContext))
            .with(object : PatternCondition<JsonProperty>("jsonObjectValue") {
                override fun accepts(jsonProperty: JsonProperty, context: ProcessingContext) =
                    jsonProperty.value is JsonObject
            })

        private val groupContextWithArray = (PlatformPatterns.psiElement(JsonProperty::class.java)
            .with(groupContext))
            .with(object : PatternCondition<JsonProperty>("jsonArrayValue") {
                override fun accepts(jsonProperty: JsonProperty, context: ProcessingContext) =
                    jsonProperty.value is JsonArray
            })

        private val additionalConfigMetadata =
            PlatformPatterns.psiFile(JsonFile::class.java)
                .with(object : PatternCondition<JsonFile>("isAdditionalJson") {
                    override fun accepts(jsonFile: JsonFile, context: ProcessingContext) =
                        SpringCoreUtil.isAdditionalConfigFile(jsonFile)
                })

        private val springConfigMetadata = PlatformPatterns.psiFile(JsonFile::class.java)
            .withName("spring-configuration-metadata.json") as PsiFilePattern.Capture<JsonFile>


        private val propertyValue = object : PatternCondition<JsonStringLiteral>("inPropertyValue") {
            override fun accepts(property: JsonStringLiteral, context: ProcessingContext) =
                JsonPsiUtil.isPropertyValue(property)
        }

        private val propertyName = object : PatternCondition<JsonProperty>("nameProperty") {
            override fun accepts(jsonProperty: JsonProperty, context: ProcessingContext) =
                NAME == jsonProperty.name
        }

        private val classPropertyNames = object : PatternCondition<JsonProperty>("classPropertyNames") {
            override fun accepts(property: JsonProperty, context: ProcessingContext) =
                TYPE == property.name || SOURCE_TYPE == property.name
        }

        private val targetProperty = object : PatternCondition<JsonProperty>("targetProperty") {
            override fun accepts(property: JsonProperty, context: ProcessingContext) =
                TARGET == property.name
        }

        private val hintsGroup = object : PatternCondition<JsonStringLiteral>("hintsGroup") {
            override fun accepts(literal: JsonStringLiteral, context: ProcessingContext) =
                context.get(groupContextKey).equals(AdditionalConfigPropertyNameReference.GroupContext.HINTS)
        }

        private val hintsProvidersGroup = object : PatternCondition<JsonStringLiteral>("hintsProvidersGroup") {
            override fun accepts(literal: JsonStringLiteral, context: ProcessingContext) =
                context.get(groupContextKey).equals(AdditionalConfigPropertyNameReference.GroupContext.HINTS_PROVIDERS)
        }

        private val replacement = object : PatternCondition<JsonProperty>("replacementProperty") {
            override fun accepts(jsonProperty: JsonProperty, context: ProcessingContext) =
                jsonProperty.name == REPLACEMENT
        }

        private val deprecation = object : PatternCondition<JsonStringLiteral>("deprecationGroup") {
            override fun accepts(literal: JsonStringLiteral, context: ProcessingContext) =
                literal.name == DEPRECATION
        }


    }

}