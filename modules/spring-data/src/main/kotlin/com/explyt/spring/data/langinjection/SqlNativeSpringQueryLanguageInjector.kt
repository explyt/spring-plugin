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

package com.explyt.spring.data.langinjection

import com.explyt.jpa.langinjection.JpqlInjectorBase
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.data.SpringDataClasses
import com.intellij.java.library.JavaLibraryUtil
import com.intellij.lang.Language
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.uast.*
import org.jetbrains.uast.expressions.UInjectionHost


class SqlNativeSpringQueryLanguageInjector : JpqlInjectorBase() {
    override fun isValidPlace(uElement: UElement): Boolean {
        return isNativeQuery(uElement) || (
                uElement is UInjectionHost && (
                        isJdbcTemplateLike(uElement) || isStringDefinedAsSqlVariable(uElement)
                )
        )
    }

    private fun isNativeQuery(uElement: UElement): Boolean {
        val parentAnnotation = uElement.getParentOfType<UAnnotation>()
            ?.takeIf { it.qualifiedName == SpringDataClasses.QUERY }
            ?: return false

        return parentAnnotation
            .findAttributeValue("nativeQuery")
            ?.evaluate() == true
    }

    private fun isStringDefinedAsSqlVariable(uElement: UInjectionHost): Boolean {
        // uElement is guaranteed to be a string literal or string template
        val parent = (uElement as? UElement)?.uastParent
        // Case 1: direct string initializer
        if (parent is UVariable && parent.name?.contains("sql", ignoreCase = true) ?: false && parent.uastInitializer == uElement) {
            return true
        }

        // Case 2: string literal is receiver of a call expression (e.g., .trimIndent()),
        val variable = parent?.uastParent as? UVariable
        if (variable != null) {
            if (variable.name?.contains("sql", ignoreCase = true) ?: false && variable.uastInitializer == parent) {
                return true
            }
        }
        return false
    }

    private fun isJdbcTemplateLike(uElement: UInjectionHost): Boolean {
        val uCallExpression = (uElement as? UElement)?.getParentOfType<UCallExpression>() ?: return false
        val expressionIndex = getExpressionIndex(uCallExpression, uElement as UElement) ?: return false

        val method = uCallExpression.tryResolve() as? PsiMethod ?: return false
        if (method.parameterList.parametersCount == 0) return false

        val parameterName = getParameterName(method, expressionIndex) ?: return false
        return parameterName.contains("sql", true)
    }

    private fun getParameterName(method: PsiMethod, expressionIndex: Int): String? {
        val parameter = method.parameterList.getParameter(expressionIndex)
        if (parameter == null) {
            val lastParameter = method.parameterList.parameters.lastOrNull()
            return lastParameter?.takeIf { it.isVarArgs }?.name
        }
        return parameter.name
    }

    private fun getExpressionIndex(uCallExpression: UCallExpression, uElement: UElement): Int? {
        for ((uExpressionIndex, uExpression) in uCallExpression.valueArguments.withIndex()) {
            if (uExpression == uElement) {
                return uExpressionIndex
            }
        }
        return null
    }

    override fun getSqlLanguage(sourcePsi: PsiElement?): Language? {
        return SqlNativeSpringQueryLanguageInjector.getSqlLanguage(sourcePsi)
    }

    companion object {
        private const val ISO_SQL = "DBN-SQL"
        private const val POSTGRES_SQL = "POSTGRES-SQL"
        private const val MYSQL_SQL = "MYSQL-SQL"
        private const val SQLITE_SQL = "SQLITE-SQL"
        private const val ORACLE_SQL = "ORACLE-SQL"

        private const val JDBC_POSTGRES = "org.postgresql:postgresql"
        private const val JDBC_MYSQL = "mysql:mysql-connector-java"
        private const val JDBC_SQLITE = "org.xerial:sqlite-jdbc"
        private const val JDBC_ORACLE_8 = "com.oracle.jdbc:ojdbc8"
        private const val JDBC_ORACLE_14 = "com.oracle.jdbc:ojdbc14"

        fun getSqlLanguage(sourcePsi: PsiElement?): Language? {
            val languageFromSettingsState = SpringToolRunConfigurationsSettingsState.getInstance().sqlLanguageId
                .takeIf { it?.isNotEmpty() == true }
                ?.let { Language.findLanguageByID(it) }
            if (languageFromSettingsState != null) return languageFromSettingsState

            val module = sourcePsi?.let { ModuleUtilCore.findModuleForPsiElement(sourcePsi) } ?: return null
            return CachedValuesManager.getManager(module.project).getCachedValue(module) {
                CachedValueProvider.Result(
                    getLanguage(module),
                    ModificationTrackerManager.getInstance(module.project).getLibraryTracker()
                )
            }
        }

        private fun getLanguage(module: Module): Language? {
            val isoSQL = Language.findLanguageByID(ISO_SQL) ?: return null
            return (if (JavaLibraryUtil.hasLibraryJar(module, JDBC_POSTGRES)) {
                Language.findLanguageByID(POSTGRES_SQL)
            } else if (JavaLibraryUtil.hasLibraryJar(module, JDBC_MYSQL)) {
                Language.findLanguageByID(MYSQL_SQL)
            } else if (JavaLibraryUtil.hasLibraryJar(module, JDBC_SQLITE)) {
                Language.findLanguageByID(SQLITE_SQL)
            } else if (JavaLibraryUtil.hasLibraryJar(module, JDBC_ORACLE_8)) {
                Language.findLanguageByID(ORACLE_SQL)
            } else if (JavaLibraryUtil.hasLibraryJar(module, JDBC_ORACLE_14)) {
                Language.findLanguageByID(ORACLE_SQL)
            } else {
                isoSQL
            }) ?: isoSQL
        }
    }
}