/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.settings.SpringPropertyFolderState
import com.explyt.spring.core.util.SpringCoreUtil.SPRING_BOOT_MAVEN
import com.intellij.ide.IconProvider
import com.intellij.java.library.JavaLibraryUtil
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.YAMLFile
import javax.swing.Icon

class SpringPropertyFileIconProvider : IconProvider() {
    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        if (element is YAMLFile || element is PropertiesFile) {
            val fileName = (element as PsiFile).name
            return getIconForFile(element, fileName)
        }
        return null
    }

    private fun getIconForFile(psiFile: PsiFile, fileName: String): Icon? {
        if (SpringPropertyFolderState.isUserPropertyFolder(psiFile)) {
            return SpringIcons.SpringSetting
        }
        if (fileName.startsWith("application-") || fileName.startsWith("application.")) {
            val module = ModuleUtilCore.findModuleForFile(psiFile) ?: return null
            if (!module.isDisposed && JavaLibraryUtil.hasLibraryJar(module, SPRING_BOOT_MAVEN)) {
                return SpringIcons.SpringSetting
            }
        }
        return null
    }
}