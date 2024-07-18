package com.esprito.spring.web.service.beans.discoverer

import com.intellij.psi.PsiClass

interface EndpointHandler {
    fun handleEndpoints(componentPsiClass: PsiClass): List<EndpointElement>
}

class EndpointHandlerChain(private val handlers: List<EndpointHandler>) {
    fun handleEndpoints(psiClass: PsiClass): List<EndpointElement> {
        return handlers.flatMap { it.handleEndpoints(psiClass) }
    }
}
