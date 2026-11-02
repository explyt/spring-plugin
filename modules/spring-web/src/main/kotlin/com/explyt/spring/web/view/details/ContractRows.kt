/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.details

import com.explyt.spring.core.service.conditional.ConditionVerdict
import com.explyt.spring.web.SpringWebBundle.message
import com.explyt.spring.web.contract.ContractStatus
import com.explyt.spring.web.contract.EndpointContract
import com.explyt.spring.web.contract.EndpointParameter
import com.explyt.spring.web.contract.EndpointSummary
import com.explyt.spring.web.contract.SchemaField
import com.explyt.spring.web.contract.ServiceCall
import com.explyt.spring.web.contract.ValueSchema
import com.explyt.spring.web.loader.EndpointAccess
import com.explyt.spring.web.loader.EndpointExposure
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer

object ContractRows {

    private const val UNKNOWN = "UNKNOWN"
    private const val UNKNOWN_SOURCE = "UNKNOWN"

    fun of(contract: EndpointContract, declaration: PsiElement): List<ContractRow> = listOfNotNull(
        requestRow(contract, declaration),
        parametersRow(contract.parameters),
        mediaTypesRow(contract.endpoint),
        actuatorRow(contract.endpoint),
        responseRow(contract),
        serviceCallsRow(contract.serviceCalls),
    )

    private fun requestRow(contract: EndpointContract, declaration: PsiElement): ContractRow {
        val endpoint = contract.endpoint
        val partial = contract.status == ContractStatus.PARTIAL
        val notes = listOfNotNull(
            endpoint.pathTemplate.takeIf { it != endpoint.fullPath }?.let { message("explyt.web.endpoints.details.template", it) },
            message("explyt.web.endpoints.details.test.source").takeIf { endpoint.testSource },
            endpoint.application?.let { message("explyt.web.endpoints.details.application", it) },
        )
        val partialNote = contract.unavailableReason?.takeIf { partial }?.let {
            ContractRow(message("explyt.web.endpoints.details.partial", it), ContractRowKind.NOTE, uncertain = true)
        }
        return ContractRow(
            text = "${endpoint.httpMethods.joinToString(" ")} ${endpoint.fullPath}",
            kind = ContractRowKind.REQUEST,
            note = notes.joinToString(", ").ifEmpty { null },
            uncertain = partial,
            target = pointerTo(declaration),
            children = listOfNotNull(partialNote),
        )
    }

    private fun parametersRow(parameters: List<EndpointParameter>): ContractRow = ContractRow(
        text = message("explyt.web.endpoints.details.parameters"),
        kind = ContractRowKind.SECTION,
        children = parameters.map(::parameterRow).ifEmpty {
            listOf(ContractRow(message("explyt.web.endpoints.details.parameters.none"), ContractRowKind.NOTE))
        },
    )

    private fun parameterRow(parameter: EndpointParameter): ContractRow {
        val requiredness = when (parameter.required) {
            true -> message("explyt.web.endpoints.details.parameter.required")
            false -> message("explyt.web.endpoints.details.parameter.optional")
            null -> message("explyt.web.endpoints.details.parameter.required.unknown")
        }
        val notes = listOfNotNull(
            parameter.source,
            parameter.type,
            requiredness,
            parameter.defaultValue?.let { message("explyt.web.endpoints.details.parameter.default", it) },
        )
        return ContractRow(
            text = parameter.name,
            kind = ContractRowKind.PARAMETER,
            note = notes.joinToString(", "),
            uncertain = parameter.source == UNKNOWN_SOURCE,
            target = parameter.declaration?.let(::pointerTo),
        )
    }

    private fun mediaTypesRow(endpoint: EndpointSummary): ContractRow? {
        val rows = endpoint.consumes.map {
            ContractRow(message("explyt.web.endpoints.details.consumes", it), ContractRowKind.MEDIA_TYPE)
        } + endpoint.produces.map {
            ContractRow(
                message("explyt.web.endpoints.details.produces", it),
                ContractRowKind.MEDIA_TYPE,
                note = endpoint.producesSource?.let { source -> message("explyt.web.endpoints.details.produces.source", source.name) },
            )
        }
        if (rows.isEmpty()) return null
        return ContractRow(message("explyt.web.endpoints.details.media.types"), ContractRowKind.SECTION, children = rows)
    }

    private fun actuatorRow(endpoint: EndpointSummary): ContractRow? {
        val rows = listOfNotNull(
            endpoint.exposure?.let {
                ContractRow(
                    message("explyt.web.endpoints.details.exposure", it.name),
                    ContractRowKind.VALUE,
                    uncertain = it == EndpointExposure.UNKNOWN,
                )
            },
            endpoint.access?.let {
                ContractRow(
                    message("explyt.web.endpoints.details.access", it.name),
                    ContractRowKind.VALUE,
                    uncertain = it == EndpointAccess.UNKNOWN,
                )
            },
            endpoint.beanCondition?.let(::conditionRow),
        )
        if (rows.isEmpty()) return null
        return ContractRow(message("explyt.web.endpoints.details.actuator"), ContractRowKind.SECTION, children = rows)
    }

    private fun conditionRow(verdict: ConditionVerdict): ContractRow? = when (verdict) {
        ConditionVerdict.Active -> null
        is ConditionVerdict.Inactive -> ContractRow(
            message(
                "explyt.web.endpoints.details.condition.inactive",
                verdict.condition.annotationFqn, verdict.condition.carrierFqn,
            ),
            ContractRowKind.VALUE,
            note = verdict.condition.detail,
        )

        is ConditionVerdict.Undecided -> verdict.conditions.firstOrNull()?.let { first ->
            ContractRow(
                message("explyt.web.endpoints.details.condition.undecided", first.annotationFqn, first.carrierFqn),
                ContractRowKind.VALUE,
                note = verdict.conditions.joinToString(", ") { it.reason.name },
                uncertain = true,
            )
        }
    }

    private fun responseRow(contract: EndpointContract): ContractRow? {
        val returnType = contract.returnType ?: return null
        val schema = contract.responseSchema
        return ContractRow(
            text = message("explyt.web.endpoints.details.response"),
            kind = ContractRowKind.SECTION,
            note = returnType,
            target = schema?.declaration?.let(::pointerTo),
            children = schema?.let(::schemaRows).orEmpty(),
        )
    }

    private fun schemaRows(schema: ValueSchema): List<ContractRow> =
        schemaNotes(schema) +
                schema.enumValues.orEmpty().map { ContractRow(it, ContractRowKind.VALUE) } +
                schema.fields.orEmpty().map(::fieldRow) +
                schema.variants.orEmpty().map(::variantRow)

    private fun schemaNotes(schema: ValueSchema): List<ContractRow> = listOfNotNull(
        schema.namingStrategy?.let {
            ContractRow(
                message("explyt.web.endpoints.details.schema.naming", it, schema.namingStrategySource.orEmpty()),
                ContractRowKind.NOTE,
                uncertain = it == UNKNOWN,
            )
        },
        schema.wireShape?.let {
            ContractRow(
                message("explyt.web.endpoints.details.schema.wire.shape", it),
                ContractRowKind.NOTE,
                note = schema.wireShapeSource,
            )
        },
        ContractRow(message("explyt.web.endpoints.details.schema.wire.shape.assumed"), ContractRowKind.NOTE, uncertain = true)
            .takeIf { schema.wireShapeAssumed == true },
        schema.schemaOmitted?.let {
            ContractRow(message("explyt.web.endpoints.details.schema.omitted", it), ContractRowKind.NOTE, uncertain = true)
        },
        ContractRow(message("explyt.web.endpoints.details.schema.additional.properties"), ContractRowKind.NOTE)
            .takeIf { schema.additionalProperties == true },
        ContractRow(
            message("explyt.web.endpoints.details.schema.polymorphic"),
            ContractRowKind.NOTE,
            note = schema.discriminator?.let { listOfNotNull(it.use, it.include, it.property).joinToString(", ") },
        ).takeIf { schema.polymorphic == true },
        schema.jsonValue?.let {
            ContractRow(
                message("explyt.web.endpoints.details.schema.json.value", it, schema.valueType.orEmpty()),
                ContractRowKind.NOTE,
            )
        },
    )

    private fun fieldRow(field: SchemaField): ContractRow {
        val nullability = when (field.nullable) {
            true -> message("explyt.web.endpoints.details.field.nullable")
            false -> message("explyt.web.endpoints.details.field.non.null")
            null -> message("explyt.web.endpoints.details.field.nullable.unknown")
        }
        val notes = listOfNotNull(
            field.type,
            nullability,
            field.declaredName?.let { message("explyt.web.endpoints.details.field.renamed", it) },
        )
        return ContractRow(
            text = field.name,
            kind = ContractRowKind.FIELD,
            note = notes.joinToString(", "),
            uncertain = field.nullable == null,
            target = field.declaration?.let(::pointerTo),
            children = field.nested?.let(::schemaRows).orEmpty(),
        )
    }

    private fun variantRow(variant: ValueSchema): ContractRow = ContractRow(
        text = variant.className,
        kind = ContractRowKind.TYPE,
        note = variant.typeId?.let { message("explyt.web.endpoints.details.schema.discriminator", it) },
        target = variant.declaration?.let(::pointerTo),
        children = schemaRows(variant),
    )

    private fun serviceCallsRow(calls: List<ServiceCall>): ContractRow? {
        if (calls.isEmpty()) return null
        return ContractRow(
            message("explyt.web.endpoints.details.service.calls"),
            ContractRowKind.SECTION,
            children = calls.map(::callRow),
        )
    }

    private fun callRow(call: ServiceCall): ContractRow = ContractRow(
        text = call.target,
        kind = ContractRowKind.CALL,
        note = listOfNotNull(
            call.callLine?.let { message("explyt.web.endpoints.details.service.call.line", it.toString()) },
            message("explyt.web.endpoints.details.service.call.unresolved").takeIf { !call.resolved },
        ).joinToString(", ").ifEmpty { null },
        uncertain = !call.resolved,
        target = call.callee?.let(::pointerTo),
    )

    private fun pointerTo(element: PsiElement): SmartPsiElementPointer<PsiElement> =
        SmartPointerManager.createPointer(element.navigationElement)
}
