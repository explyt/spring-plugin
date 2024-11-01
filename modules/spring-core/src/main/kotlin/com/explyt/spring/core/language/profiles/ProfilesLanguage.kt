package com.explyt.spring.core.language.profiles

import com.intellij.lang.Language


class ProfilesLanguage private constructor() : Language("Profiles") {

    companion object {
        @JvmField
        val INSTANCE = ProfilesLanguage()
    }

}