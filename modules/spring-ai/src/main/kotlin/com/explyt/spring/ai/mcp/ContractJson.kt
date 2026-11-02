/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.service.conditional.ConditionVerdict
import com.explyt.spring.web.contract.EndpointContract
import com.explyt.spring.web.contract.EndpointParameter
import com.explyt.spring.web.contract.EndpointSummary
import com.explyt.spring.web.contract.SchemaDiscriminator
import com.explyt.spring.web.contract.SchemaField
import com.explyt.spring.web.contract.ServiceCall
import com.explyt.spring.web.contract.ValueSchema

internal fun EndpointSummary.toCompactJson() = CompactEndpointJson(
    httpMethods = httpMethods,
    fullPath = fullPath,
    pathTemplate = pathTemplate.takeIf { it != fullPath },
    controllerClass = controllerClass,
    methodName = methodName,
    filePath = position.filePath,
    library = position.library,
    line = position.line,
    endpointType = type.readable,
    consumes = consumes,
    produces = produces,
    producesSource = producesSource?.name,
    exposed = exposure?.name,
    access = access?.name,
    beanCondition = beanCondition?.toBeanConditionJson(),
    testSource = testSource.takeIf { it },
    application = application,
)

internal fun EndpointContract.toJson(): EndpointContractJson {
    val core = endpoint.toCompactJson()
    val calls = serviceCalls.map { it.toJson() }
    return EndpointContractJson(
        httpMethods = core.httpMethods,
        fullPath = core.fullPath,
        pathTemplate = core.pathTemplate,
        controllerClass = core.controllerClass,
        methodName = core.methodName,
        filePath = core.filePath,
        library = core.library,
        line = core.line,
        parameters = parameters.map { it.toJson() },
        returnType = returnType,
        responseSchema = responseSchema?.toJson(),
        produces = core.produces,
        producesSource = core.producesSource,
        consumes = core.consumes,
        serviceCall = calls.firstOrNull(),
        serviceCalls = calls,
        endpointType = core.endpointType,
        exposed = core.exposed,
        access = core.access,
        beanCondition = core.beanCondition,
        testSource = core.testSource,
        contractStatus = status.name,
        contractUnavailableReason = unavailableReason,
        application = core.application,
    )
}

internal fun EndpointParameter.toJson() = EndpointParameterJson(
    name = name,
    source = source,
    type = type,
    required = required,
    defaultValue = defaultValue,
)

private fun ServiceCall.toJson() = ServiceCallJson(
    target = target,
    filePath = position.filePath,
    library = position.library,
    line = position.line,
    callLine = callLine,
    resolved = if (resolved) null else false,
)

private fun ConditionVerdict.toBeanConditionJson(): EndpointBeanConditionJson? = when (this) {
    ConditionVerdict.Active -> null
    is ConditionVerdict.Inactive -> EndpointBeanConditionJson(
        state = "INACTIVE",
        annotation = condition.annotationFqn,
        carrier = condition.carrierFqn,
        detail = condition.detail,
        reasons = null,
    )
    is ConditionVerdict.Undecided -> conditions.firstOrNull().let { first ->
        EndpointBeanConditionJson(
            state = "UNDECIDED",
            annotation = first?.annotationFqn,
            carrier = first?.carrierFqn,
            detail = first?.detail,
            reasons = conditions.map { it.reason.name },
        )
    }
}

internal fun ValueSchema.toJson(): DtoSchemaJson = DtoSchemaJson(
    className = className,
    typeId = typeId,
    fields = fields?.map { it.toJson() },
    enumValues = enumValues,
    jsonValue = jsonValue,
    valueType = valueType,
    enumConstants = enumConstants,
    namingStrategy = namingStrategy,
    namingStrategySource = namingStrategySource,
    schemaOmitted = schemaOmitted,
    additionalProperties = additionalProperties,
    polymorphic = polymorphic,
    discriminator = discriminator?.toJson(),
    variants = variants?.map { it.toJson() },
    wireShape = wireShape,
    wireShapeSource = wireShapeSource,
    wireShapeAssumed = wireShapeAssumed,
)

private fun SchemaDiscriminator.toJson() = DiscriminatorJson(use = use, include = include, property = property)

private fun SchemaField.toJson() = DtoFieldJson(
    name = name,
    declaredName = declaredName,
    type = type,
    nullable = nullable,
    nested = nested?.toJson(),
)
