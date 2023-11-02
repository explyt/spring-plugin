package com.esprito.spring.core.language.profiles.psi

import com.esprito.spring.core.language.profiles.ProfilesLanguage
import com.intellij.psi.tree.IElementType

class ProfilesElementType(debugName: String) :
    IElementType(debugName, ProfilesLanguage.INSTANCE)