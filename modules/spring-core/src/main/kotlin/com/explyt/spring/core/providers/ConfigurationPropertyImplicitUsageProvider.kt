/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers

import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.lang.properties.codeInspection.unused.ImplicitPropertyUsageProvider
import com.intellij.lang.properties.psi.Property

class ConfigurationPropertyImplicitUsageProvider : ImplicitPropertyUsageProvider {
    override fun isUsed(property: Property): Boolean {
        return SpringCoreUtil.isConfigurationPropertyFile(property.propertiesFile.containingFile)
    }
}
