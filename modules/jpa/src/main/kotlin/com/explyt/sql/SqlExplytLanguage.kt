/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.sql

import com.intellij.lang.Language

class SqlExplytLanguage : Language("SQL-EXPLYT") {

    companion object {
        @JvmField
        val INSTANCE = SqlExplytLanguage()
    }
}