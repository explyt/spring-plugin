/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.language.http

import com.explyt.spring.web.language.http.parser.HttpParser
import com.explyt.spring.web.language.http.psi.HttpFile
import com.explyt.spring.web.language.http.psi.HttpTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class HttpParserDefinition : ParserDefinition {

    override fun createLexer(project: Project): Lexer = HttpLexerAdapter()

    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY//HttpTokenSets.IDENTIFIERS

    override fun createParser(project: Project): PsiParser = HttpParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun createFile(viewProvider: FileViewProvider): PsiFile = HttpFile(viewProvider)

    override fun createElement(node: ASTNode): PsiElement = HttpTypes.Factory.createElement(node)

    companion object {
        val FILE = IFileElementType(HttpLanguage.INSTANCE)
    }

}