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

class SpringBeanData @PropertyMapping(
    "beanName", "className", "scope", "methodName", "methodType", "type", "primary", "rootBean", "projectBean"
) constructor(
    val beanName: String,
    val className: String,
    val scope: String,
    val methodName: String?,
    val methodType: String?,
    val type: SpringBeanType,
    val primary: Boolean,
    val rootBean: Boolean,
    val projectBean: Boolean
) : AbstractExternalEntityData(SYSTEM_ID) {
    companion object {
        val KEY = Key.create(
            SpringBeanData::class.java,
            ProjectKeys.PROJECT.processingWeight + 1
        )
    }

    override fun toString(): String {
        return "SpringBeanData(beanName='$beanName', className='$className')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as SpringBeanData

        if (beanName != other.beanName) return false
        if (className != other.className) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + beanName.hashCode()
        result = 31 * result + className.hashCode()
        return result
    }


}

enum class SpringBeanType {
    APPLICATION, CONTROLLER, REPOSITORY, SERVICE, COMPONENT, AUTO_CONFIGURATION,
    CONFIGURATION, CONFIGURATION_PROPERTIES, METHOD, OTHER, MESSAGE_MAPPING, ASPECT
}