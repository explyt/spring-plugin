package com.esprito.spring.web.service.beans.discoverer

import com.esprito.spring.web.util.SpringWebUtil
import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.searches.AnnotatedElementsSearch

interface SpringWebEndpointsLoader {

    fun searchEndpoints(module: Module): List<EndpointElement>

    fun getType(): EndpointType

    fun getEndpointElements(urlPath: String, module: Module): List<EndpointElement> {
        val searchUrl = SpringWebUtil.simplifyUrl(urlPath)

        return searchEndpoints(module)
            .filter { SpringWebUtil.isEndpointMatches(it.path, searchUrl) }
    }

    fun searchAnnotatedClasses(annotation: PsiClass, module: Module): List<PsiClass> {
        return AnnotatedElementsSearch.searchPsiClasses(annotation, module.moduleWithDependenciesScope)
            .filter { !it.isAnnotationType }
    }

    companion object {
        val EP_NAME = ProjectExtensionPointName<SpringWebEndpointsLoader>(
            "com.esprito.spring.web.springWebEndpointsLoader"
        )

    }
}

data class Referrer(
    val path: String,
    val method: String?,
    val psiElement: PsiElement
)

data class EndpointElement(
    val path: String,
    val requestMethods: List<String>,
    val psiElement: PsiElement,
    val containingClass: PsiClass?,
    val containingFile: PsiFile?,
    val type: EndpointType
)

sealed class EndpointResult {
    data class ReferrerResult(val referrer: Referrer) : EndpointResult()
    data class EndpointElementResult(val endpointElement: EndpointElement) : EndpointResult()
}

enum class EndpointType {
    SPRING_MVC,
    SPRING_WEBFLUX,
    OPENAPI
}
