/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.jpa.ql

import com.intellij.lang.Language

class JpqlLanguage : Language("JPQL-EXPLYT") {

    companion object {
        @JvmField
        val INSTANCE = JpqlLanguage()
    }
}