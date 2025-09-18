/*
 * Copyright © 2025 Explyt Ltd
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