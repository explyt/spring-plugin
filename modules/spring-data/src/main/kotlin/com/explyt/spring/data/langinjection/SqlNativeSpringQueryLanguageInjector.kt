/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.data.langinjection

import com.explyt.jpa.langinjection.JpqlInjectorBase
import com.explyt.plugin.PluginSqlLanguage
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.data.SpringDataClasses
import com.explyt.sql.SqlExplytLanguage
import com.explyt.util.ExplytDbConstants
import com.intellij.java.library.JavaLibraryUtil
import com.intellij.lang.Language
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.codeStyle.NameUtil
import org.jetbrains.uast.*


class SqlNativeSpringQueryLanguageInjector : JpqlInjectorBase() {
    override fun isValidPlace(uElement: UElement): Boolean = try {
        isValidPlaceSafely(uElement)
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (_: RuntimeException) {
        false
    } catch (_: AssertionError) {
        false
    }

    private fun isValidPlaceSafely(uElement: UElement): Boolean {
        if (isNativeQuery(uElement) || isJdbcTemplateLike(uElement)) {
            return !PluginSqlLanguage.SPRING_QL.isEnabled()
        }
        return isStringDefinedAsSqlVariable(uElement)
    }

    private fun isNativeQuery(uElement: UElement): Boolean {
        val parentAnnotation = uElement.getParentOfType<UAnnotation>()
            ?.takeIf { it.qualifiedName == SpringDataClasses.QUERY }
            ?: return false

        return parentAnnotation
            .findAttributeValue("nativeQuery")
            ?.evaluate() == true
    }

    private fun String.hasSqlWord(): Boolean =
        NameUtil.nameToWordsLowerCase(this).any { it == SQL_WORD }

    private fun isStringDefinedAsSqlVariable(uElement: UElement): Boolean {
        val parent = uElement.uastParent
        // Case 1: direct string initializer
        if (uElement is ULiteralExpression || uElement is UPolyadicExpression) {
            if (isNotSqlString(uElement)) return false
        }
        if (parent is UVariable && parent.name?.hasSqlWord() ?: false && parent.uastInitializer == uElement) {
            return true
        }

        // Case 2: string literal is receiver of a call expression (e.g., .trimIndent()),
        val variable = parent?.uastParent as? UVariable ?: return false
        return variable.name?.hasSqlWord() ?: false && variable.uastInitializer == parent
    }

    private fun isNotSqlString(uElement: UExpression): Boolean {
        val text = uElement.evaluateString() ?: return false
        return text.isNotEmpty() && !text.contains(' ')
    }

    private fun isJdbcTemplateLike(uElement: UElement): Boolean {
        val uCallExpression = uElement.getParentOfType<UCallExpression>() ?: return false
        val expressionIndex = getExpressionIndex(uCallExpression, uElement) ?: return false
        val method = uCallExpression.tryResolve() as? PsiMethod ?: return false
        val parameterName = getParameterName(method, expressionIndex) ?: return false

        return parameterName.hasSqlWord()
    }

    private fun getParameterName(method: PsiMethod, expressionIndex: Int): String? {
        val parameter = method.parameterList.getParameter(expressionIndex)
        if (parameter != null) return parameter.name

        return method.parameterList.parameters
            .lastOrNull()
            ?.takeIf { it.isVarArgs }
            ?.name
    }

    private fun getExpressionIndex(uCallExpression: UCallExpression, uElement: UElement): Int? {
        for ((uExpressionIndex, uExpression) in uCallExpression.valueArguments.withIndex()) {
            if (uExpression == uElement) {
                return uExpressionIndex
            }
        }
        return null
    }

    override fun getSqlLanguage(sourcePsi: PsiElement?): Language {
        return getSqlLanguage()
    }

    companion object {
        private const val SQL_WORD = "sql"
        private const val ISO_SQL = "DBN-SQL"
        private const val POSTGRES_SQL = "POSTGRES-SQL"
        private const val MYSQL_SQL = "MYSQL-SQL"
        private const val SQLITE_SQL = "SQLITE-SQL"
        private const val ORACLE_SQL = "ORACLE-SQL"

        private const val SQL_METHOD = "sql"
        private const val EXECUTE_METHOD = "execute"
        private const val QUERY_METHOD = "query"
        private const val QUERY_FOR_OBJECT_METHOD = "queryForObject"
        private const val QUERY_FOR_LIST_METHOD = "queryForList"
        private const val QUERY_FOR_MAP_METHOD = "queryForMap"
        private const val QUERY_FOR_ROW_SET_METHOD = "queryForRowSet"
        private const val QUERY_FOR_STREAM_METHOD = "queryForStream"
        private const val UPDATE_METHOD = "update"
        private const val BATCH_UPDATE_METHOD = "batchUpdate"

        private val SQL_FIRST_ARGUMENT_METHODS = setOf(
            SQL_METHOD,
            EXECUTE_METHOD,
            QUERY_METHOD,
            QUERY_FOR_OBJECT_METHOD,
            QUERY_FOR_LIST_METHOD,
            QUERY_FOR_MAP_METHOD,
            QUERY_FOR_ROW_SET_METHOD,
            QUERY_FOR_STREAM_METHOD,
            UPDATE_METHOD
        )
        private val SQL_VARARG_ARGUMENT_METHODS = setOf(BATCH_UPDATE_METHOD)

        fun getSqlLanguage(): Language {
            val languageFromSettingsState = SpringToolRunConfigurationsSettingsState.getInstance().sqlLanguageId
                .takeIf { it?.isNotEmpty() == true }
                ?.let { Language.findLanguageByID(it) }
            if (languageFromSettingsState != null) return languageFromSettingsState
            return SqlExplytLanguage.INSTANCE
        }

        private fun getLanguage(module: Module): Language? {
            val isoSQL = Language.findLanguageByID(ISO_SQL) ?: return null
            return (if (JavaLibraryUtil.hasLibraryJar(module, ExplytDbConstants.JDBC_POSTGRES)) {
                Language.findLanguageByID(POSTGRES_SQL)
            } else if (JavaLibraryUtil.hasLibraryJar(module, ExplytDbConstants.JDBC_MYSQL)) {
                Language.findLanguageByID(MYSQL_SQL)
            } else if (JavaLibraryUtil.hasLibraryJar(module, ExplytDbConstants.JDBC_SQLITE)) {
                Language.findLanguageByID(SQLITE_SQL)
            } else if (JavaLibraryUtil.hasLibraryJar(module, ExplytDbConstants.JDBC_ORACLE_8)) {
                Language.findLanguageByID(ORACLE_SQL)
            } else if (JavaLibraryUtil.hasLibraryJar(module, ExplytDbConstants.JDBC_ORACLE_14)) {
                Language.findLanguageByID(ORACLE_SQL)
            } else {
                isoSQL
            }) ?: isoSQL
        }
    }
}