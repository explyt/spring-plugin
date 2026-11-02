/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.details

import com.explyt.spring.web.contract.EndpointContracts
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointVerbs
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.view.EndpointElementViewData
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager

object EndpointDetailsReader {

    fun detailsOf(row: EndpointElementViewData, project: Project): EndpointDetails {
        val endpoint = endpointOf(row, project) ?: return EndpointDetails.NotFound
        if (!endpoint.type.isWeb) return EndpointDetails.NoHttpContract(endpoint.type.readable)
        val contract = EndpointContracts.contractOf(endpoint, project)
        return EndpointDetails.Contract(ContractRows.of(contract, endpoint.psiElement))
    }

    fun endpointOf(row: EndpointElementViewData, project: Project): EndpointElement? {
        val declaration = row.psiPointer.element ?: return null
        val psiManager = PsiManager.getInstance(project)
        return SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints().firstOrNull { endpoint ->
            endpoint.type == row.type &&
                    endpoint.path == row.path &&
                    row.method in EndpointVerbs.labelsOf(endpoint) &&
                    endpoint.application?.qualifiedName == row.application &&
                    (endpoint.containingClass?.name ?: endpoint.containingFile?.name) == row.classOrFileName &&
                    psiManager.areElementsEquivalent(endpoint.psiElement, declaration)
        }
    }
}
