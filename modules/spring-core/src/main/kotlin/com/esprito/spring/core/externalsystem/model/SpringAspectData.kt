package com.esprito.spring.core.externalsystem.model

import com.esprito.spring.core.externalsystem.utils.Constants.SYSTEM_ID
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