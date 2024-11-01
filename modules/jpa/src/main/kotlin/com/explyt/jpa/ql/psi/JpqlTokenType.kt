package com.explyt.jpa.ql.psi

import com.explyt.jpa.ql.JpqlLanguage.Companion.INSTANCE
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls

class JpqlTokenType(debugName: @NonNls String) : IElementType(debugName, INSTANCE)