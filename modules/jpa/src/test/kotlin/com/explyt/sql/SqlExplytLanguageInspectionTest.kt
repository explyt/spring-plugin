/*
 * Copyright © 2025 Explyt Ltd
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
        myFixture.configureByText("sql.esql", "select * from<EOLError/>")
        myFixture.testHighlighting("sql.esql")
    }

    fun `test where param ?`() {
        myFixture.configureByText("sql.esql", "select * from table where id = ?")
        myFixture.testHighlighting("sql.esql")
    }

    fun `test where param name`() {
        myFixture.configureByText("sql.esql", "select * from table where id = :id")
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