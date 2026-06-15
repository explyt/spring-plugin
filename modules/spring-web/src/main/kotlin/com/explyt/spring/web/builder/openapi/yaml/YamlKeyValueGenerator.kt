/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi.yaml

import com.intellij.openapi.project.Project
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.psi.YAMLKeyValue

interface YamlKeyValueGenerator {
    fun toYamlKeyValue(keyName: String, project: Project): YAMLKeyValue {
        return YAMLElementGenerator.getInstance(project)
            .createYamlKeyValue(
                keyName,
                toString().removePrefix("\n")
            )
    }
}