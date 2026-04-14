/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.jpa.ql.psi

import com.explyt.jpa.ql.JpqlLanguage.Companion.INSTANCE
import com.intellij.psi.tree.IElementType

class JpqlTokenType(debugName: String) : IElementType(debugName, INSTANCE)