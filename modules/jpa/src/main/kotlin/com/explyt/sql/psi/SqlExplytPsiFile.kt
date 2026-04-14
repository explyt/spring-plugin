/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.sql.psi

import com.explyt.sql.SqlExplytFileType
import com.explyt.sql.SqlExplytLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class SqlExplytPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, SqlExplytLanguage.INSTANCE) {
    override fun getFileType(): FileType {
        return SqlExplytFileType.INSTANCE
    }

    override fun toString(): String {
        return "Sql Explyt File"
    }
}