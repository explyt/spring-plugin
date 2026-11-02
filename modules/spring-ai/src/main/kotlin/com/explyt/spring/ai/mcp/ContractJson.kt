/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.web.contract.SchemaDiscriminator
import com.explyt.spring.web.contract.SchemaField
import com.explyt.spring.web.contract.ValueSchema

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
