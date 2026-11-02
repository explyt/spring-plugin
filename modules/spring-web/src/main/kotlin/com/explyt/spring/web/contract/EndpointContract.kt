/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.contract

import com.explyt.spring.core.service.conditional.ConditionVerdict
import com.explyt.spring.core.source.SourcePosition
import com.explyt.spring.web.loader.EndpointAccess
import com.explyt.spring.web.loader.EndpointExposure
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.ProducesSource
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

data class EndpointSummary(
    val httpMethods: List<String>,
    val fullPath: String,
    val pathTemplate: String,
    val controllerClass: String?,
    val methodName: String?,
    val position: SourcePosition,
    val type: EndpointType,
    val consumes: List<String>,
    val produces: List<String>,
    val producesSource: ProducesSource?,
    val exposure: EndpointExposure?,
    val access: EndpointAccess?,
    val beanCondition: ConditionVerdict?,
    val testSource: Boolean,
    val application: String?,
)

data class EndpointParameter(
    val name: String,
    val source: String,
    val type: String,
    val required: Boolean?,
    val defaultValue: String? = null,
    val declaration: PsiElement? = null,
)

data class ServiceCall(
    val target: String,
    val position: SourcePosition,
    val callLine: Int? = null,
    val resolved: Boolean = true,
    val callee: PsiMethod? = null,
)

enum class ContractStatus { COMPLETE, PARTIAL }

data class EndpointContract(
    val endpoint: EndpointSummary,
    val parameters: List<EndpointParameter>,
    val returnType: String?,
    val responseSchema: ValueSchema?,
    val serviceCalls: List<ServiceCall>,
    val status: ContractStatus,
    val unavailableReason: String?,
)
