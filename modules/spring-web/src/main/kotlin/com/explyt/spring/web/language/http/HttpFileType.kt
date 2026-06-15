/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.language.http

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class HttpFileType private constructor() : LanguageFileType(HttpLanguage.INSTANCE) {

    override fun getName(): String = "Http File"
    override fun getDescription(): String = "Http language file"
    override fun getDefaultExtension(): String = "http"
    override fun getIcon(): Icon = AllIcons.General.Web

    companion object {
        @JvmField
        val INSTANCE = HttpFileType()
    }

}