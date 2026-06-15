/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.language.http.psi

import com.explyt.spring.web.language.http.HttpFileType
import com.explyt.spring.web.language.http.HttpLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class HttpFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, HttpLanguage.INSTANCE) {

    override fun getFileType() = HttpFileType.INSTANCE
    override fun toString() = "Http File"
}