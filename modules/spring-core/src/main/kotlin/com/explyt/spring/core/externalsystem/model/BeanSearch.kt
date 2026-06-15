/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.model

import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData
import com.intellij.serialization.PropertyMapping

class BeanSearch @PropertyMapping("enabled", "projectPath") constructor(
    var enabled: Boolean,
    val projectPath: String
) : AbstractExternalEntityData(SYSTEM_ID) {
    companion object {
        val KEY = Key.create(
            BeanSearch::class.java,
            ProjectKeys.PROJECT.processingWeight + 1
        )
    }

    override fun toString(): String {
        return "BeanSearch($enabled')"
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (javaClass != obj?.javaClass) return false
        if (!super.equals(obj)) return false

        obj as BeanSearch

        if (enabled != obj.enabled) return false
        if (projectPath != obj.projectPath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + projectPath.hashCode()
        return result
    }
}
