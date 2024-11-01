package com.explyt.jpa.ql.psi

import com.explyt.jpa.ql.JpqlLanguage
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls

class JpqlElementType(debugName: @NonNls String) : IElementType(debugName, JpqlLanguage.INSTANCE)