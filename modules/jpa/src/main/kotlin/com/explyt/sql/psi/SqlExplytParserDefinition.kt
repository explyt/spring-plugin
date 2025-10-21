/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
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