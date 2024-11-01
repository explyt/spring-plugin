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

package com.explyt.jpa.ql.psi

import com.intellij.psi.tree.TokenSet

object JpqlTokensSets {

    @JvmField
    val IDENTIFIERS = TokenSet.create(
        JpqlTypes.IDENTIFIER,
    )

    @JvmField
    val OPERATORS = TokenSet.create(
        JpqlTypes.LT,
        JpqlTypes.LTE,
        JpqlTypes.GT,
        JpqlTypes.GTE,
        JpqlTypes.EQ,
        JpqlTypes.NEQ,
    )

    @JvmField
    val SEPARATORS = TokenSet.create(
        JpqlTypes.LPAREN,
        JpqlTypes.RPAREN,
        JpqlTypes.COLON,
        JpqlTypes.COMMA,
        JpqlTypes.DOT,
        JpqlTypes.PLUS,
        JpqlTypes.MINUS,
        JpqlTypes.MUL,
        JpqlTypes.DIV,
        JpqlTypes.LT,
        JpqlTypes.LTE,
        JpqlTypes.GT,
        JpqlTypes.GTE,
        JpqlTypes.NEQ
    )

    val KEYWORDS = TokenSet.create(
        JpqlTypes.ABS,
        JpqlTypes.ALL,
        JpqlTypes.AND,
        JpqlTypes.ANY,
        JpqlTypes.AS,
        JpqlTypes.ASC,
        JpqlTypes.AVG,
        JpqlTypes.BETWEEN,
        JpqlTypes.BOOLEAN_LITERAL,
        JpqlTypes.BOTH,
        JpqlTypes.BY,
        JpqlTypes.CASE,
        JpqlTypes.COALESCE,
        JpqlTypes.CONCAT,
        JpqlTypes.COUNT,
        JpqlTypes.CURRENT_DATE,
        JpqlTypes.CURRENT_TIME,
        JpqlTypes.CURRENT_TIMESTAMP,
        JpqlTypes.DELETE,
        JpqlTypes.DESC,
        JpqlTypes.DISTINCT,
        JpqlTypes.ELSE,
        JpqlTypes.EMPTY,
        JpqlTypes.END,
        JpqlTypes.ENTRY,
        JpqlTypes.ESCAPE,
        JpqlTypes.EXISTS,
        JpqlTypes.FETCH,
        JpqlTypes.FUNCTION,
        JpqlTypes.FROM,
        JpqlTypes.GROUP,
        JpqlTypes.HAVING,
        JpqlTypes.IN,
        JpqlTypes.INDEX,
        JpqlTypes.INNER,
        JpqlTypes.INSERT,
        JpqlTypes.INTO,
        JpqlTypes.IS,
        JpqlTypes.JOIN,
        JpqlTypes.KEY,
        JpqlTypes.LEADING,
        JpqlTypes.LEFT,
        JpqlTypes.LENGTH,
        JpqlTypes.LIMIT,
        JpqlTypes.LIKE,
        JpqlTypes.LOCATE,
        JpqlTypes.LOWER,
        JpqlTypes.MAX,
        JpqlTypes.MEMBER,
        JpqlTypes.MIN,
        JpqlTypes.MINUS,
        JpqlTypes.NEW,
        JpqlTypes.NOT,
        JpqlTypes.NULL,
        JpqlTypes.NULLIF,
        JpqlTypes.OBJECT,
        JpqlTypes.OF,
        JpqlTypes.OFFSET,
        JpqlTypes.ON,
        JpqlTypes.OR,
        JpqlTypes.ORDER,
        JpqlTypes.OUTER,
        JpqlTypes.SELECT,
        JpqlTypes.SET,
        JpqlTypes.SIZE,
        JpqlTypes.SOME,
        JpqlTypes.SQRT,
        JpqlTypes.SUBSTRING,
        JpqlTypes.SUM,
        JpqlTypes.THEN,
        JpqlTypes.TRAILING,
        JpqlTypes.TRIM,
        JpqlTypes.TYPE,
        JpqlTypes.UPDATE,
        JpqlTypes.UPPER,
        JpqlTypes.VALUE,
        JpqlTypes.VALUES,
        JpqlTypes.WITH,
        JpqlTypes.WHEN,
        JpqlTypes.WHERE,
    )

    @JvmField
    val COMMENTS = TokenSet.create()

    val STRING_LITERALS = TokenSet.create(JpqlTypes.STRING)

    val NUMERIC_LITERALS = TokenSet.create(JpqlTypes.NUMERIC)

    val DATETIME_LITERALS = TokenSet.create(JpqlTypes.DATETIME)
}
