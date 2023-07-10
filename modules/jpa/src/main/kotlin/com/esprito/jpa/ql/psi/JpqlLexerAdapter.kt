package com.esprito.jpa.ql.psi

import com.esprito.jpa.ql._JpqlLexer
import com.intellij.lexer.FlexAdapter

class JpqlLexerAdapter : FlexAdapter(_JpqlLexer(null))