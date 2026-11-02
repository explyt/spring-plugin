/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.contract

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement

data class ValueSchema(
    val className: String,
    val typeId: String? = null,
    val fields: List<SchemaField>?,
    val enumValues: List<String>? = null,
    val jsonValue: String? = null,
    val valueType: String? = null,
    val enumConstants: List<String>? = null,
    val namingStrategy: String? = null,
    val namingStrategySource: String? = null,
    val schemaOmitted: String? = null,
    val additionalProperties: Boolean? = null,
    val polymorphic: Boolean? = null,
    val discriminator: SchemaDiscriminator? = null,
    val variants: List<ValueSchema>? = null,
    val wireShape: String? = null,
    val wireShapeSource: String? = null,
    val wireShapeAssumed: Boolean? = null,
    val declaration: PsiClass? = null,
)

data class SchemaDiscriminator(
    val use: String,
    val include: String?,
    val property: String?,
)

data class SchemaField(
    val name: String,
    val declaredName: String?,
    val type: String,
    val nullable: Boolean?,
    val nested: ValueSchema?,
    val declaration: PsiElement? = null,
)
