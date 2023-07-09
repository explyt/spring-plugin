package com.esprito.jpql.psi

import com.esprito.jpql._JpqlLexer
import com.intellij.lexer.FlexAdapter

class JpqlLexerAdapter : FlexAdapter(_JpqlLexer(null))