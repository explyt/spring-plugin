package com.esprito.spring.core.properties

import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.SpringRunConfigurationBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory

class FilePropertyTemplateGroupFactory : FileTemplateGroupDescriptorFactory {
    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        val template = FileTemplateDescriptor(SpringProperties.SPRING_XML_TEMPLATE)
        val groupDescriptor = FileTemplateGroupDescriptor(
            SpringRunConfigurationBundle.message("esprito.spring.group.property.xml"), AllIcons.FileTypes.Xml)
        groupDescriptor.addTemplate(template)
        return groupDescriptor
    }
}