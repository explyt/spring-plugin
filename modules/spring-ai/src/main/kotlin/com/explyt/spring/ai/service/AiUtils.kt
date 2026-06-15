/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.service

import com.explyt.spring.ai.action.JAKARTA_ENTITY
import com.explyt.spring.ai.action.JPA_ENTITY
import com.explyt.util.ModuleUtil
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.util.allScope

object AiUtils {
    fun getLanguageSentence(project: Project, file: VirtualFile): String {
        val language = ModuleUtilCore.findModuleForFile(file, project)
            ?.let {
                if (ModuleUtil.isKotlinModule(it))
                    KotlinLanguage.INSTANCE.displayName else JavaLanguage.INSTANCE.displayName
            } ?: JavaLanguage.INSTANCE.displayName
        return "$language language."
    }

    fun getLanguageSentence(psiElement: PsiElement): String {
        val language = if (psiElement.language == KotlinLanguage.INSTANCE)
            KotlinLanguage.INSTANCE.displayName else JavaLanguage.INSTANCE.displayName
        return "$language language."
    }

    fun getJpaEntitySentence(project: Project): String {
        val isJakarta = JavaPsiFacade.getInstance(project).findClass(JAKARTA_ENTITY, project.allScope()) != null
        return if (isJakarta) JAKARTA_ENTITY else JPA_ENTITY
    }
}