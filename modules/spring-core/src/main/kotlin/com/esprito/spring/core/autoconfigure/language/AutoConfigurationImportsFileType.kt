package com.esprito.spring.core.autoconfigure.language

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.SpringProperties
import com.intellij.lang.properties.PropertiesLanguage
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile
import com.intellij.openapi.vfs.VirtualFile

object AutoConfigurationImportsFileType : LanguageFileType(PropertiesLanguage.INSTANCE, true),
    FileTypeIdentifiableByVirtualFile {
    override fun getName() = SpringProperties.AUTOCONFIGURATION_IMPORTS

    override fun getDescription() = SpringProperties.AUTOCONFIGURATION_IMPORTS

    override fun getDefaultExtension() = ""

    override fun getIcon() = SpringIcons.SpringFactories

    override fun getDisplayName() = "Auto configuration imports properties"

    override fun isMyFileType(file: VirtualFile): Boolean {
        val parent = file.parent ?: return false
        return SpringProperties.AUTOCONFIGURATION_IMPORTS == file.name && parent.name == SpringProperties.SPRING
                && parent.parent?.name == SpringProperties.META_INF
    }
}