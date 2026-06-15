/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.language.http.psi

import com.explyt.spring.web.language.http.HttpLanguage
import com.intellij.psi.tree.IElementType

class HttpElementType(debugName: String) :
    IElementType(debugName, HttpLanguage.INSTANCE)