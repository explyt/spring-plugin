/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties

import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.SpringRunConfigurationBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory

class FilePropertyTemplateGroupFactory : FileTemplateGroupDescriptorFactory {
    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        val template = FileTemplateDescriptor(SpringProperties.SPRING_XML_TEMPLATE)
        val groupDescriptor = FileTemplateGroupDescriptor(
            SpringRunConfigurationBundle.message("explyt.spring.group.property.xml"), AllIcons.FileTypes.Xml
        )
        groupDescriptor.addTemplate(template)
        return groupDescriptor
    }
}