/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.language.http

import com.intellij.lang.Language


class HttpLanguage private constructor() : Language("HTTP-EXPLYT") {

    companion object {
        @JvmField
        val INSTANCE = HttpLanguage()
    }

}