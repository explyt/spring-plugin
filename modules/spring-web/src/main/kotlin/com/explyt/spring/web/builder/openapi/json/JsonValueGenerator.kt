/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi.json

import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.project.Project

interface JsonValueGenerator {

    fun toJsonValue(keyName: String, project: Project): JsonProperty {
        return JsonElementGenerator(project)
            .createProperty(keyName, "{${toString()}}")
    }

}