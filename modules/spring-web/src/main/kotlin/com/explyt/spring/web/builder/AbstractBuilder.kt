/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder

abstract class AbstractBuilder(
    protected var indent: String = "",
    protected val builder: StringBuilder = StringBuilder()
) {
    abstract fun build()

    override fun toString(): String {
        build()
        return builder.toString()
    }

    protected fun addLinesWithIndent(from: String, indent: String) {
        for (line in from.trimIndent().lines()) {
            builder.appendLine()
            builder.append("${indent}$line")
        }
    }

}