package com.esprito.spring.core.language.profiles.psi

import com.esprito.spring.core.language.profiles.ProfilesFileType
import com.esprito.spring.core.language.profiles.ProfilesLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class ProfilesFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, ProfilesLanguage.INSTANCE) {

    override fun getFileType() = ProfilesFileType.INSTANCE
    override fun toString() = "Profiles File"
}