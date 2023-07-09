package com.esprito.jpql.psi

import com.esprito.jpql.JpqlLanguage.Companion.INSTANCE
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls

class JpqlElementType(debugName: @NonNls String) : IElementType(debugName, INSTANCE)