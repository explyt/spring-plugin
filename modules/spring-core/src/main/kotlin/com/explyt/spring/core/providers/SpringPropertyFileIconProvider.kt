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

package com.explyt.spring.core.providers

import com.explyt.spring.core.SpringIcons
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
        if (fileName.startsWith("application-") || fileName.startsWith("application.")) {
            val module = ModuleUtilCore.findModuleForFile(psiFile) ?: return null
            if (!module.isDisposed && JavaLibraryUtil.hasLibraryJar(module, SPRING_BOOT_MAVEN)) {
                return SpringIcons.SpringSetting
            }
        }
        return null
    }
}