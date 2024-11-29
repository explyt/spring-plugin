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

package com.explyt.spring.core.autoconfigure

import com.explyt.spring.core.autoconfigure.language.AutoConfigurationImportsFileType
import com.explyt.spring.core.autoconfigure.language.FactoriesFileType
import com.intellij.lang.properties.codeInspection.unused.ImplicitPropertyUsageProvider
import com.intellij.lang.properties.psi.Property
import com.intellij.openapi.fileTypes.FileTypeRegistry

class AutoConfigureImplicitPropertyUsageProvider : ImplicitPropertyUsageProvider {
    override fun isUsed(property: Property) =
        FileTypeRegistry.getInstance().isFileOfType(property.propertiesFile.virtualFile, FactoriesFileType)
                || FileTypeRegistry.getInstance()
            .isFileOfType(property.propertiesFile.virtualFile, AutoConfigurationImportsFileType)
}