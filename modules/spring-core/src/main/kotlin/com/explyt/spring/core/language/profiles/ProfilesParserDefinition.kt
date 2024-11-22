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

package com.explyt.spring.core.language.profiles

import com.explyt.spring.core.language.profiles.parser.ProfilesParser
import com.explyt.spring.core.language.profiles.psi.ProfilesFile
import com.explyt.spring.core.language.profiles.psi.ProfilesTokenSets
import com.explyt.spring.core.language.profiles.psi.ProfilesTypes
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