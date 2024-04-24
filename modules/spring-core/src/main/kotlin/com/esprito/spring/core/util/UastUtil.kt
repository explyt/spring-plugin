package com.esprito.spring.core.util

import com.esprito.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.evaluateString

object UastUtil {
    fun UExpression.getPropertyValue(): String? {
        val value = this.evaluateString() ?: return null
        if (value.startsWith("#{")) return null
        if (value.startsWith("\${") && value.endsWith("}")) {
            val psiElement = this.sourcePsi ?: return null
            val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return null
            return DefinedConfigurationPropertiesSearch.getInstance(module.project)
                .findProperties(module, value.substring(2, value.length - 1))
                .firstOrNull()?.value
        }
        return value
    }
}