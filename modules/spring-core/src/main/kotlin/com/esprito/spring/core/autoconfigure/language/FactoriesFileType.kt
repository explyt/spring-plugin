package com.esprito.spring.core.autoconfigure.language

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.SpringProperties.META_INF
import com.esprito.spring.core.SpringProperties.SPRING_FACTORIES_FILE_NAME
import com.intellij.lang.properties.PropertiesLanguage
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile
import com.intellij.openapi.vfs.VirtualFile


object FactoriesFileType : LanguageFileType(PropertiesLanguage.INSTANCE, true), FileTypeIdentifiableByVirtualFile {
    override fun getName() = SPRING_FACTORIES_FILE_NAME

    override fun getDescription() = SPRING_FACTORIES_FILE_NAME

    override fun getDefaultExtension() = ""

    override fun getIcon() = SpringIcons.SpringFactories

    override fun getDisplayName() = "Factories configuration properties"

    override fun isMyFileType(file: VirtualFile) =
        SPRING_FACTORIES_FILE_NAME == file.name && file.parent?.name == META_INF
}