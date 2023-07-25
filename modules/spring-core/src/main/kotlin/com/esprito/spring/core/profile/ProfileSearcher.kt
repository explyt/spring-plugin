package com.esprito.spring.core.profile

import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.openapi.module.Module

interface ProfileSearcher {
    fun searchProfiles(module: Module): List<String>

    companion object {
        val EP_NAME = ProjectExtensionPointName<ProfileSearcher>("com.esprito.spring.core.profileSearcher")
    }
}