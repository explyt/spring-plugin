/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
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
