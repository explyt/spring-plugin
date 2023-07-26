package com.esprito.jpa.ql.psi

import com.esprito.jpa.ql.JpqlLanguage
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls

class JpqlElementType(debugName: @NonNls String) : IElementType(debugName, JpqlLanguage.INSTANCE)