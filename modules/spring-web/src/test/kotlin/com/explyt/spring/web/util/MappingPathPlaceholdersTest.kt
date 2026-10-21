/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MappingPathPlaceholdersTest {

    private val configuration = mapOf(
        "app.path" to "/l",
        "app.nested" to "\${app.path}/x",
        "app.loop" to "\${app.loop}",
    )

    private fun resolve(path: String) = MappingPathPlaceholders.resolve(path, configuration::get)

    @Test
    fun `a defined key is replaced by its value`() {
        assertEquals("/l/{code}", resolve("\${app.path}/{code}"))
    }

    @Test
    fun `a defined key wins over the default`() {
        assertEquals("/l/{code}", resolve("\${app.path:/d}/{code}"))
    }

    @Test
    fun `an undefined key falls back to its default`() {
        assertEquals("/d/{code}", resolve("\${app.missing:/d}/{code}"))
    }

    @Test
    fun `an unresolvable placeholder is kept as written`() {
        assertEquals("\${app.missing}/{code}", resolve("\${app.missing}/{code}"))
    }

    @Test
    fun `a value holding placeholders is resolved too`() {
        assertEquals("/l/x", resolve("\${app.nested}"))
    }

    @Test
    fun `a self-referencing value stops instead of looping`() {
        assertEquals("\${app.loop}", resolve("\${app.loop}"))
    }

    @Test
    fun `a path template variable is not a placeholder`() {
        assertEquals("/api/{id}", resolve("/api/{id}"))
    }

    @Test
    fun `a placeholder nested in a default falls back to the inner default`() {
        assertEquals("/d", resolve("\${outer:\${inner:/d}}"))
    }

    @Test
    fun `a placeholder nested in a default takes the inner key value`() {
        assertEquals("/l", resolve("\${outer:\${app.path:/d}}"))
    }

    @Test
    fun `a defined outer key wins over a nested default`() {
        assertEquals("/l", resolve("\${app.path:\${inner:/d}}"))
    }

    @Test
    fun `a nested placeholder without any value is kept as written`() {
        assertEquals("\${outer:\${inner}}", resolve("\${outer:\${inner}}"))
    }
}
