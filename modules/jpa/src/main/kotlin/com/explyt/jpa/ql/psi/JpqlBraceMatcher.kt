package com.explyt.jpa.ql.psi

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class JpqlBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = arrayOf(
        BracePair(
            JpqlTypes.LPAREN, JpqlTypes.RPAREN, true
        )
    )

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean {
        return TokenType.WHITE_SPACE === contextType
                || contextType === JpqlTypes.SEMICOLON
                || contextType === JpqlTypes.COMMA
                || contextType === JpqlTypes.RPAREN
                || null == contextType
    }

    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int = openingBraceOffset
}