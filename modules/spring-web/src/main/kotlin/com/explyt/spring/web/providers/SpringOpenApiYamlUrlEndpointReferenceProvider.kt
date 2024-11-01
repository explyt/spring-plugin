package com.explyt.spring.web.providers

import com.explyt.spring.web.references.ExplytControllerMethodReference
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.SpringWebUtil.PATHS
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue

class SpringOpenApiYamlUrlEndpointReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(psiElement: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val keyElement = psiElement as? YAMLKeyValue ?: return emptyArray()
        val parentElement = psiElement.parentOfType<YAMLKeyValue>(false) ?: return emptyArray()
        if (YAMLUtil.getConfigFullName(parentElement) == PATHS) {
            return getReferenceForUrl(keyElement)
        }

        return getReferenceForRequestMethod(keyElement, parentElement)
    }

    private fun getReferenceForUrl(keyElement: YAMLKeyValue): Array<PsiReference> {
        val key = YAMLUtil.getConfigFullNameParts(keyElement).lastOrNull() ?: return emptyArray()

        return arrayOf(
            ExplytControllerMethodReference(
                keyElement,
                key,
                null,
                ElementManipulators.getValueTextRange(keyElement),
                true
            )
        )
    }

    private fun getReferenceForRequestMethod(keyElement: YAMLKeyValue, urlElement: YAMLKeyValue): Array<PsiReference> {
        val url = YAMLUtil.getConfigFullNameParts(urlElement).lastOrNull() ?: return emptyArray()
        val key = YAMLUtil.getConfigFullNameParts(keyElement).lastOrNull() ?: return emptyArray()
        if (key !in SpringWebUtil.REQUEST_METHODS) return emptyArray()
        val pathElement = urlElement.parentOfType<YAMLKeyValue>() ?: return emptyArray()
        if (YAMLUtil.getConfigFullName(pathElement) != PATHS) return emptyArray()

        return arrayOf(
            ExplytControllerMethodReference(
                keyElement,
                url,
                key.uppercase(),
                ElementManipulators.getValueTextRange(keyElement),
                true
            )
        )
    }

}