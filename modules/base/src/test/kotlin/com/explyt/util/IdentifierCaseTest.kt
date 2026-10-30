/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

class IdentifierCaseTest {

    @Test
    fun `lowers only the first character`() {
        assertEquals("userController", "UserController".lowercaseFirstChar())
    }

    @Test
    fun `keeps an acronym after the first character as Kotlin Lombok did`() {
        assertEquals("uRLService", "URLService".lowercaseFirstChar())
    }

    @Test
    fun `leaves a lowercase start and an empty name unchanged`() {
        assertEquals("server", "server".lowercaseFirstChar())
        assertEquals("", "".lowercaseFirstChar())
    }

    @Test
    fun `does not depend on the default locale`() {
        val defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("tr"))
        try {
            assertEquals("index", "Index".lowercaseFirstChar())
        } finally {
            Locale.setDefault(defaultLocale)
        }
    }
}
