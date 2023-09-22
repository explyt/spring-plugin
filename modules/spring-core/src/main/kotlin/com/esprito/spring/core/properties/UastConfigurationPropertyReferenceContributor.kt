package com.esprito.spring.core.properties

import com.esprito.spring.core.SpringCoreClasses
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.*

class UastConfigurationPropertyReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(injectionHostUExpression(), ConfigurationPropertyReferenceProvider())
    }
}

class ConfigurationPropertyReferenceProvider : UastInjectionHostReferenceProvider() {

    companion object {
        val PROPERTIES_PATTERN = "\\\$\\{([.A-z\\d_-]+)(:[^{]*)?\\s*\\}".toPattern()
    }

    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val expression = uExpression as? ULiteralExpression ?: return PsiReference.EMPTY_ARRAY
        val uAnnotation = expression.getParentOfType<UAnnotation>() ?: return PsiReference.EMPTY_ARRAY
        if (SpringCoreClasses.VALUE != uAnnotation.qualifiedName) { //TODO: search for meta annotations
            return PsiReference.EMPTY_ARRAY
        }
        val valueText = uAnnotation.findDeclaredAttributeValue("value")?.evaluateString()
            ?: return PsiReference.EMPTY_ARRAY
        val referenceProperties = extractReferenceProperty(valueText)

        if (referenceProperties.isEmpty() && valueText.startsWith("\${")) {
            return arrayOf(
                EspritoPropertyReference(
                    host, "", TextRange.from(3, 0)
                )
            )
        }

        return referenceProperties
            .map { referenceProperty ->
                val startOffset = host.text.indexOf(valueText)
                val range = referenceProperty.textRange

                EspritoPropertyReference(
                    host, referenceProperty.key, TextRange(
                        startOffset + range.startOffset,
                        startOffset + range.endOffset
                    )
                )
            }.toTypedArray()
    }

    private fun extractReferenceProperty(text: String): List<ReferenceProperty> {
        val matcher = PROPERTIES_PATTERN.matcher(text)
        val properties = mutableListOf<ReferenceProperty>()
        while (matcher.find()) {
            val propertyKey = matcher.group(1)

            val keyValue = ReferenceProperty(
                key = propertyKey,
                textRange = TextRange(
                    matcher.start(1),
                    matcher.end(1)
                )
            )
            properties.add(keyValue)
        }
        return properties
    }
}

data class ReferenceProperty(
    val key: String,
    val textRange: TextRange,
)

