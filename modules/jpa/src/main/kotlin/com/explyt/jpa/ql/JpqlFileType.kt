/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.jpa.ql

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class JpqlFileType private constructor() : LanguageFileType(JpqlLanguage.INSTANCE) {
    override fun getName(): String = "JPA QL Explyt"

    override fun getDescription(): String = "JPQL file Explyt"

    override fun getDefaultExtension(): String = "ejpql"

    override fun getIcon(): Icon = AllIcons.FileTypes.Text

    companion object {
        @JvmField val INSTANCE = JpqlFileType()
    }
}