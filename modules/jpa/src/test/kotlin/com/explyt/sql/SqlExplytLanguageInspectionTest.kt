/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.sql

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary

class SqlExplytLanguageInspectionTest : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7, TestLibrary.springJdbc_6_2_5
    )

    fun testSimpleSelect() {
        myFixture.configureByText("sql.esql", "select * from table")
        myFixture.testHighlighting("sql.esql")
    }

    fun testError() {
        myFixture.configureByText("sql.esql", "<error>select</error> * fro")
        myFixture.testHighlighting("sql.esql")
    }

    fun `test where param question mark`() {
        myFixture.configureByText("sql.esql", "select * from table where id = ?")
        myFixture.testHighlighting("sql.esql")
    }

    fun `test where param name`() {
        myFixture.configureByText("sql.esql", "select * from table where id = :id")
        myFixture.testHighlighting("sql.esql")
    }

    fun `test complex join select`() {
        val sqlText = """
            SELECT *, N.* FROM (SELECT * FROM NN WHERE n > 0) AS N LEFT JOIN C ON C.id=b.id 
    INNER JOIN F ON f.id=c.f 
    INNER JOIN cC AS C11 ON f.id=c.f
    INNER JOIN (SELECT * FROM MM) AS M ON m.id=f.id
        """.trimIndent()
        myFixture.configureByText("sql.esql", sqlText)
        myFixture.testHighlighting("sql.esql")
    }

    fun `test insert into`() {
        myFixture.configureByText(
            "sql.esql", "INSERT INTO Employees (FirstName, LastName, Department)\n" +
                    "VALUES (?, ?, ?)"
        )
        myFixture.testHighlighting("sql.esql")
    }

    fun `test update`() {
        myFixture.configureByText("sql.esql", "update users set name = ? where id=:id")
        myFixture.testHighlighting("sql.esql")
    }

    fun `test delete`() {
        myFixture.configureByText("sql.esql", "delete from users u where u.id=?")
        myFixture.testHighlighting("sql.esql")
    }
}