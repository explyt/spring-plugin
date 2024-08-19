package com.esprito.spring.web.providers

import com.esprito.spring.web.references.EspritoControllerMethodReference
import com.esprito.spring.web.util.SpringWebUtil
import com.esprito.spring.web.util.SpringWebUtil.OPEN_API
import com.esprito.spring.web.util.SpringWebUtil.PATHS
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext

class SpringOpenApiJsonUrlEndpointReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(psiElement: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val nameElement = psiElement as? JsonStringLiteral ?: return emptyArray()
        val jsonFile = nameElement.containingFile as? JsonFile ?: return emptyArray()
        if (!isOpenApiFile(jsonFile)) return emptyArray()

        val jsonProperty = nameElement.parentOfType<JsonProperty>() ?: return emptyArray()
        if (jsonProperty.nameElement != nameElement) return emptyArray()

        val parentProperty = jsonProperty.parentOfType<JsonProperty>() ?: return emptyArray()
        if (parentProperty.name == PATHS) {
            return getReferenceForUrl(nameElement)
        }

        return getReferenceForRequestMethod(nameElement, parentProperty)
    }

    private fun getReferenceForUrl(nameElement: JsonStringLiteral): Array<PsiReference> {
        val key = nameElement.value

        return arrayOf(
            EspritoControllerMethodReference(
                nameElement,
                key,
                null,
                ElementManipulators.getValueTextRange(nameElement),
                true
            )
        )
    }

    private fun getReferenceForRequestMethod(
        nameElement: JsonStringLiteral,
        urlProperty: JsonProperty
    ): Array<PsiReference> {
        val url = urlProperty.name
        val key = nameElement.value

        if (key !in SpringWebUtil.REQUEST_METHODS) return emptyArray()
        val pathElement = urlProperty.parentOfType<JsonProperty>() ?: return emptyArray()
        if (pathElement.name != PATHS) return emptyArray()

        return arrayOf(
            EspritoControllerMethodReference(
                nameElement,
                url,
                key.uppercase(),
                ElementManipulators.getValueTextRange(nameElement),
                true
            )
        )
    }

    private fun isOpenApiFile(jsonFile: JsonFile): Boolean {
        return jsonFile.topLevelValue?.childrenOfType<JsonProperty>()
            ?.any { it.name == OPEN_API } ?: false
    }

}