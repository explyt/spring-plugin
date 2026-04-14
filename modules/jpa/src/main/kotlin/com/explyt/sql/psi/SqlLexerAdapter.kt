/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.sql.psi

import com.explyt.sql._SqlLexer
import com.intellij.lexer.FlexAdapter

class SqlLexerAdapter : FlexAdapter(_SqlLexer(null))