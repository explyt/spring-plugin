package com.esprito.spring.core.language.profiles

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class ProfilesFileType private constructor() : LanguageFileType(ProfilesLanguage.INSTANCE) {

    override fun getName(): String = "Profiles File"
    override fun getDescription(): String = "Profiles language file"
    override fun getDefaultExtension(): String = "profiles"
    override fun getIcon(): Icon = AllIcons.General.User

    companion object {
        @JvmField
        val INSTANCE = ProfilesFileType()
    }

}