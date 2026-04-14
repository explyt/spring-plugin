/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.sql.psi

import com.explyt.sql.SqlExplytLanguage
import com.explyt.sql.parser.SqlParser
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

class SqlExplytParserDefinition : ParserDefinition {
    override fun createLexer(project: Project): Lexer = SqlLexerAdapter()

    override fun getCommentTokens(): TokenSet = SqlTokensSets.COMMENTS

    override fun getStringLiteralElements(): TokenSet = SqlTokensSets.STRING_LITERALS

    override fun createParser(project: Project): PsiParser = SqlParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun createFile(viewProvider: FileViewProvider): PsiFile = SqlExplytPsiFile(viewProvider)

    override fun createElement(node: ASTNode): PsiElement = SqlTypes.Factory.createElement(node)

    companion object {
        val FILE = IFileElementType(SqlExplytLanguage.INSTANCE)
    }
}