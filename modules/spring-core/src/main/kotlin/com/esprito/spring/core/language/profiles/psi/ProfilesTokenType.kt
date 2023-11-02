package com.esprito.spring.core.language.profiles.psi

import com.esprito.spring.core.language.profiles.ProfilesLanguage
import com.intellij.psi.tree.IElementType

class ProfilesTokenType(debugName: String) :
    IElementType(debugName, ProfilesLanguage.INSTANCE) {

    override fun toString(): String =
        "ProfilesTokenType." + super.toString()

}