/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.intellij.psi.PsiClass

interface EndpointHandler {
    fun handleEndpoints(componentPsiClass: PsiClass): List<EndpointElement>
}

class EndpointHandlerChain(private val handlers: List<EndpointHandler>) {
    fun handleEndpoints(psiClass: PsiClass): List<EndpointElement> {
        return handlers.flatMap { it.handleEndpoints(psiClass) }
    }
}
