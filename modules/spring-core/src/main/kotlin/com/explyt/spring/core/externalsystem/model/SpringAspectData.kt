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

class SpringAspectData @PropertyMapping(
    "aspectQualifiedClassName", "aspectMethodName", "beanQualifiedClassName", "beanMethodName", "methodQualifiedParams"
) constructor(
    val aspectQualifiedClassName: String,
    val aspectMethodName: String,
    val beanQualifiedClassName: String,
    var beanMethodName: String,
    var methodQualifiedParams: List<String>,
) : AbstractExternalEntityData(SYSTEM_ID) {
    companion object {
        val KEY = Key.create(
            SpringAspectData::class.java,
            ProjectKeys.PROJECT.processingWeight + 2
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as SpringAspectData

        if (aspectQualifiedClassName != other.aspectQualifiedClassName) return false
        if (aspectMethodName != other.aspectMethodName) return false
        if (beanQualifiedClassName != other.beanQualifiedClassName) return false
        if (beanMethodName != other.beanMethodName) return false
        if (methodQualifiedParams != other.methodQualifiedParams) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + aspectQualifiedClassName.hashCode()
        result = 31 * result + aspectMethodName.hashCode()
        result = 31 * result + beanQualifiedClassName.hashCode()
        result = 31 * result + beanMethodName.hashCode()
        result = 31 * result + methodQualifiedParams.hashCode()
        return result
    }

    override fun toString(): String {
        return "AopData(aspectClass='$aspectQualifiedClassName', aspectMethod='$aspectMethodName', " +
                "beanClass='$beanQualifiedClassName', beanMethodName='$beanMethodName')"
    }

}