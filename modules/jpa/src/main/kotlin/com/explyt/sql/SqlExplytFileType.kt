/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.sql

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class SqlExplytFileType private constructor() : LanguageFileType(SqlExplytLanguage.INSTANCE) {
    override fun getName(): String = "SQL Explyt"

    override fun getDescription(): String = "SQL Explyt file"

    override fun getDefaultExtension(): String = "esql"

    override fun getIcon(): Icon = AllIcons.FileTypes.Text

    companion object {
        @JvmField
        val INSTANCE = SqlExplytFileType()
    }
}