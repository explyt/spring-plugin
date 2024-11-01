package com.explyt.spring.core.properties.providers

import com.intellij.util.containers.ContainerUtil

enum class SpringMetadataValueProvider(val id: String, val description: String, val parameters: Array<Parameter>) {
    ANY(
        "any",
        "Permits any additional value to be provided.",
        emptyArray()
    ),
    CLASS_REFERENCE(
        "class-reference",
        "Auto-completes the classes available in the project.",
        arrayOf(Parameter("target", true), Parameter("concrete", false))
    ),
    HANDLE_AS(
        "handle-as",
        "Handles the property as if it were defined by the type defined by the mandatory target parameter.",
        arrayOf(Parameter("target", true))
    ),
    LOGGER_NAME(
        "logger-name",
        "Auto-completes valid logger names and logger groups.",
        arrayOf(Parameter("group", false))
    ),
    SPRING_BEAN_REFERENCE(
        "spring-bean-reference",
        "Auto-completes the available bean names in the current project.",
        arrayOf(Parameter("target", true))
    ),
    SPRING_PROFILE_NAME(
        "spring-profile-name",
        "Auto-completes the available Spring profile names in the project.",
        emptyArray()
    );

    fun isRequiredParameters(): Boolean {
        return parameters.any { it.required }
    }

    companion object {
        fun findById(id: String): SpringMetadataValueProvider? {
            return ContainerUtil.find(entries.toTypedArray()) { it.id == id }
        }
    }

    class Parameter(nameParameter: String, requiredParameter: Boolean) {
        val name: String = nameParameter
        val required: Boolean = requiredParameter
    }


}