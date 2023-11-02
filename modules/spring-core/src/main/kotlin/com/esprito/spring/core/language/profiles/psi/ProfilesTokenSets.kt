package com.esprito.spring.core.language.profiles.psi

import com.intellij.psi.tree.TokenSet

object ProfilesTokenSets {

    @JvmField
    val SEPARATORS = TokenSet.create(
        ProfilesTypes.LPAREN,
        ProfilesTypes.RPAREN
    )

    @JvmField
    val OPERATORS = TokenSet.create(
        ProfilesTypes.AND,
        ProfilesTypes.OR,
        ProfilesTypes.NOT
    )

    @JvmField
    val IDENTIFIERS = TokenSet.create(
        ProfilesTypes.VALUE
    )

}