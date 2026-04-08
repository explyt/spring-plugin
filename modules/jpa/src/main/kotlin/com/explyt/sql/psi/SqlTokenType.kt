/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.sql.psi

import com.explyt.sql.SqlExplytLanguage
import com.intellij.psi.tree.IElementType

class SqlTokenType(debugName: String) : IElementType(debugName, SqlExplytLanguage.INSTANCE)