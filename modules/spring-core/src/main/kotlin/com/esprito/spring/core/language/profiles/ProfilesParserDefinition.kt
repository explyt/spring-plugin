package com.esprito.spring.core.language.profiles

import com.esprito.spring.core.language.profiles.parser.ProfilesParser
import com.esprito.spring.core.language.profiles.psi.ProfilesFile
import com.esprito.spring.core.language.profiles.psi.ProfilesTokenSets
import com.esprito.spring.core.language.profiles.psi.ProfilesTypes
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

class ProfilesParserDefinition : ParserDefinition {

    override fun createLexer(project: Project): Lexer = ProfilesLexerAdapter()

    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

    override fun getStringLiteralElements(): TokenSet = ProfilesTokenSets.IDENTIFIERS

    override fun createParser(project: Project): PsiParser = ProfilesParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun createFile(viewProvider: FileViewProvider): PsiFile = ProfilesFile(viewProvider)

    override fun createElement(node: ASTNode): PsiElement = ProfilesTypes.Factory.createElement(node)

    companion object {
        val FILE = IFileElementType(ProfilesLanguage.INSTANCE)
    }

}