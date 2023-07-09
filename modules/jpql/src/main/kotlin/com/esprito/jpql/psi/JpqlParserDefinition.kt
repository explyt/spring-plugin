package com.esprito.jpql.psi

import com.esprito.jpql.JpqlLanguage
import com.esprito.jpql.parser.JpqlParser
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

class JpqlParserDefinition : ParserDefinition {
    override fun createLexer(project: Project): Lexer = JpqlLexerAdapter()

    override fun getCommentTokens(): TokenSet = JpqlTokensSets.COMMENTS

    override fun getStringLiteralElements(): TokenSet = JpqlTokensSets.STRING_LITERALS

    override fun createParser(project: Project): PsiParser = JpqlParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun createFile(viewProvider: FileViewProvider): PsiFile = JpqlFile(viewProvider)

    override fun createElement(node: ASTNode): PsiElement = JpqlTypes.Factory.createElement(node)

    companion object {
        val FILE = IFileElementType(JpqlLanguage.INSTANCE)
    }
}