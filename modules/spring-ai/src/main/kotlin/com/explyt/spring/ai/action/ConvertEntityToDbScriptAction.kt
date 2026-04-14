/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.action

import com.explyt.spring.ai.SpringAiBundle
import com.explyt.spring.ai.service.AiPluginService
import com.explyt.spring.core.util.ActionUtil
import com.explyt.util.ExplytDbConstants
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.java.library.JavaLibraryUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement

class ConvertEntityToDbScriptAction : AnAction(SpringAiBundle.message("explyt.spring.ai.action.jpa.to.db")) {
    override fun update(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (virtualFiles == null || virtualFiles.size > 1) {
            ActionUtil.isEnabledAndVisible(e, false)
            return
        }

        val uClasses = virtualFiles
            .mapNotNull { it.toPsiFile(project)?.toUElement() as? UFile }
            .flatMap { it.classes }

        val enabled = uClasses.isNotEmpty() && uClasses.any { it.javaPsi.isMetaAnnotatedBy(JPA_ANNOTATIONS) }

        e.presentation.isEnabledAndVisible = enabled
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return

        val dtoPsiClasses = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
            ?.mapNotNull { it.toPsiFile(project)?.toUElement() as? UFile }
            ?.flatMap { it.classes }
            ?.filter { it.javaPsi.isMetaAnnotatedBy(JPA_ANNOTATIONS) } ?: return
        val uClass = dtoPsiClasses.firstOrNull() ?: return
        val module = ModuleUtilCore.findModuleForPsiElement(uClass.javaPsi) ?: return

        val entityName = uClass.javaPsi.name
        val virtualFiles = dtoPsiClasses.mapNotNull { it.javaPsi.containingFile?.virtualFile }
        val dbTypeSentence = getDbTypeSentence(module)
        val dbToolSentence = getDbToolSentence(module)
        val liquibaseFormatAskSentence = getLiquibaseFormatAskSentence(dbToolSentence)
        val prompt = """
            Create new `DB Script` from this `JPA Entity class` $entityName.         
            $dbTypeSentence $dbToolSentence Save result files to the corresponding directory. 
            $liquibaseFormatAskSentence                                     
        """.trimIndent()
        AiPluginService.getInstance(project).performPrompt(prompt, virtualFiles)
    }

    private fun getDbToolSentence(module: Module): String {
        return if (JavaLibraryUtil.hasLibraryJar(module, ExplytDbConstants.LIQUIBASE_CORE)) {
            "Liquibase Tool."
        } else if (JavaLibraryUtil.hasLibraryJar(module, ExplytDbConstants.FLYWAY_CORE)) {
            "Flyway Tool."
        } else {
            "Plain SQL."
        }
    }

    private fun getLiquibaseFormatAskSentence(dbToolSentence: String): String {
        return if (dbToolSentence.contains("Liquibase"))
            "Ask me what Liquibase format i want: XML, YAML, JSON, SQL?" else ""
    }

    private fun getDbTypeSentence(module: Module): String {
        return if (JavaLibraryUtil.hasLibraryJar(module, ExplytDbConstants.JDBC_POSTGRES)) {
            "Postgres DB."
        } else if (JavaLibraryUtil.hasLibraryJar(module, ExplytDbConstants.JDBC_MYSQL)) {
            "MySql DB."
        } else if (JavaLibraryUtil.hasLibraryJar(module, ExplytDbConstants.JDBC_SQLITE)) {
            "SqlLite DB."
        } else if (JavaLibraryUtil.hasLibraryJar(module, ExplytDbConstants.JDBC_ORACLE_8)) {
            "Oracle DB."
        } else if (JavaLibraryUtil.hasLibraryJar(module, ExplytDbConstants.JDBC_ORACLE_14)) {
            "Oracle DB."
        } else if (JavaLibraryUtil.hasLibraryJar(module, ExplytDbConstants.JDBC_H2)) {
            "H2 DB."
        } else {
            ""
        }
    }
}