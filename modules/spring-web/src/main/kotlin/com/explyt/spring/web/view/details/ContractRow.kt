/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.details

import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer

data class ContractRow(
    val text: String,
    val kind: ContractRowKind,
    val note: String? = null,
    val uncertain: Boolean = false,
    val target: SmartPsiElementPointer<out PsiElement>? = null,
    val children: List<ContractRow> = emptyList(),
)

enum class ContractRowKind { REQUEST, SECTION, PARAMETER, MEDIA_TYPE, TYPE, FIELD, VALUE, CALL, NOTE }

sealed interface EndpointDetails {
    data class Contract(val rows: List<ContractRow>) : EndpointDetails
    data class NoHttpContract(val endpointType: String) : EndpointDetails
    data object NotFound : EndpointDetails
}
