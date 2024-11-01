package com.explyt.jpa.ql.psi

import com.explyt.jpa.ql._JpqlLexer
import com.intellij.lexer.FlexAdapter

class JpqlLexerAdapter : FlexAdapter(_JpqlLexer(null))