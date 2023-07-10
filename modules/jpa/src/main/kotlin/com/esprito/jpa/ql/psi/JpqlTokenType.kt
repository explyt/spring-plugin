package com.esprito.jpa.ql.psi

import com.esprito.jpa.ql.JpqlLanguage.Companion.INSTANCE
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls

class JpqlTokenType(debugName: @NonNls String) : IElementType(debugName, INSTANCE) {
}