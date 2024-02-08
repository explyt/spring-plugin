package com.esprito.spring.core.properties.providers

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.properties.references.EspritoPropertyReference
import com.esprito.spring.core.providers.ConfigurationPropertyLineMarkerProvider
import com.esprito.spring.core.util.SpringCoreUtil.removeDummyIdentifier
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReference.EMPTY_ARRAY
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.*

class ValueConfigurationPropertyReferenceProvider : UastInjectionHostReferenceProvider() {

    companion object {
        val PROPERTIES_PATTERN = """\$\{([.A-z\d_-]+)(:[^{]*)?\s*\}""".toPattern()
    }

    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        if (uExpression !is ULiteralExpression && uExpression !is UPolyadicExpression) return EMPTY_ARRAY

        val uAnnotation = uExpression.getParentOfType<UAnnotation>() ?: return EMPTY_ARRAY
        if (SpringCoreClasses.VALUE != uAnnotation.qualifiedName) {
            return EMPTY_ARRAY
        }

        val valueText = uAnnotation.findDeclaredAttributeValue("value")
            ?.evaluateString()
            ?.removeDummyIdentifier() ?: return EMPTY_ARRAY
        val referenceProperties = extractReferenceProperty(valueText)

        if (referenceProperties.isEmpty() && valueText.startsWith("\${")) {
            val startPosition =
                if (uExpression.lang == ConfigurationPropertyLineMarkerProvider.KOTLIN_LANGUAGE.value) 4 else 3
            return arrayOf(
                EspritoPropertyReference(
                    host, "", TextRange.from(startPosition, 0)
                )
            )
        }

        return referenceProperties
            .map { referenceProperty ->
                val text = host.text.removeDummyIdentifier()
                val startOffset = text.indexOf(referenceProperty.key)

                EspritoPropertyReference(
                    host, referenceProperty.key,
                    TextRange.from(startOffset, referenceProperty.key.length)
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

