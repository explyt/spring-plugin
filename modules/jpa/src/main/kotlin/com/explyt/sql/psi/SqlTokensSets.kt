/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.sql.psi

import com.intellij.psi.tree.TokenSet

object SqlTokensSets {

    @JvmField
    val IDENTIFIERS = TokenSet.create(
        SqlTypes.IDENTIFIER,
    )

    @JvmField
    val OPERATORS = TokenSet.create(
        SqlTypes.LT,
        SqlTypes.LTE,
        SqlTypes.GT,
        SqlTypes.GTE,
        SqlTypes.EQ,
        SqlTypes.NEQ,
    )

    @JvmField
    val SEPARATORS = TokenSet.create(
        SqlTypes.LPAREN,
        SqlTypes.RPAREN,
        SqlTypes.COLON,
        SqlTypes.COMMA,
        SqlTypes.DOT,
        SqlTypes.PLUS,
        SqlTypes.MINUS,
        SqlTypes.MUL,
        SqlTypes.DIV,
        SqlTypes.LT,
        SqlTypes.LTE,
        SqlTypes.GT,
        SqlTypes.GTE,
        SqlTypes.NEQ
    )

    val KEYWORDS = TokenSet.create(
        SqlTypes.ABS,
        SqlTypes.ALL,
        SqlTypes.AND,
        SqlTypes.ANY,
        SqlTypes.AS,
        SqlTypes.ASC,
        SqlTypes.AVG,
        SqlTypes.BETWEEN,
        SqlTypes.BOOLEAN_LITERAL,
        SqlTypes.BOTH,
        SqlTypes.BY,
        SqlTypes.CASE,
        SqlTypes.COALESCE,
        SqlTypes.CONCAT,
        SqlTypes.COUNT,
        SqlTypes.CURRENT_DATE,
        SqlTypes.CURRENT_TIME,
        SqlTypes.CURRENT_TIMESTAMP,
        SqlTypes.DELETE,
        SqlTypes.DESC,
        SqlTypes.DISTINCT,
        SqlTypes.ELSE,
        SqlTypes.EMPTY,
        SqlTypes.END,
        SqlTypes.ENTRY,
        SqlTypes.ESCAPE,
        SqlTypes.EXISTS,
        SqlTypes.FUNCTION,
        SqlTypes.FROM,
        SqlTypes.GROUP,
        SqlTypes.HAVING,
        SqlTypes.IN,
        SqlTypes.INDEX,
        SqlTypes.INNER,
        SqlTypes.INSERT,
        SqlTypes.INTO,
        SqlTypes.IS,
        SqlTypes.JOIN,
        SqlTypes.KEY,
        SqlTypes.LEADING,
        SqlTypes.LEFT,
        SqlTypes.LENGTH,
        SqlTypes.LIMIT,
        SqlTypes.LIKE,
        SqlTypes.LOCATE,
        SqlTypes.LOWER,
        SqlTypes.MAX,
        SqlTypes.MEMBER,
        SqlTypes.MIN,
        SqlTypes.MINUS,
        SqlTypes.NOT,
        SqlTypes.NULL,
        SqlTypes.NULLIF,
        SqlTypes.OBJECT,
        SqlTypes.OF,
        SqlTypes.OFFSET,
        SqlTypes.ON,
        SqlTypes.OR,
        SqlTypes.ORDER,
        SqlTypes.OUTER,
        SqlTypes.SELECT,
        SqlTypes.SET,
        SqlTypes.SIZE,
        SqlTypes.SOME,
        SqlTypes.SQRT,
        SqlTypes.SUBSTRING,
        SqlTypes.SUM,
        SqlTypes.THEN,
        SqlTypes.TRAILING,
        SqlTypes.TRIM,
        SqlTypes.TYPE,
        SqlTypes.UPDATE,
        SqlTypes.UPPER,
        SqlTypes.VALUE,
        SqlTypes.VALUES,
        SqlTypes.WHEN,
        SqlTypes.WHERE,
    )

    @JvmField
    val COMMENTS = TokenSet.create()

    val STRING_LITERALS = TokenSet.create(SqlTypes.STRING)

    val NUMERIC_LITERALS = TokenSet.create(SqlTypes.NUMERIC)

    val DATETIME_LITERALS = TokenSet.create(SqlTypes.DATETIME)
}
