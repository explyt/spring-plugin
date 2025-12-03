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

package com.explyt.inspection

import com.explyt.base.LibraryClassCache
import com.explyt.plugin.PluginIds
import com.explyt.util.SpringBaseClasses.CORE_ENVIRONMENT
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.psi.PsiFile

abstract class SpringBaseUastLocalInspectionTool : AbstractBaseUastLocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        //spring data is ultimate plugin. it is mean that inspection not available in Paid version,
        // because it already has spring bundle plugin
        return PluginIds.SPRING_DATA_JB.isNotEnabled() && LibraryClassCache.searchForLibraryClass(
            file.project,
            CORE_ENVIRONMENT
        ) != null
    }
}