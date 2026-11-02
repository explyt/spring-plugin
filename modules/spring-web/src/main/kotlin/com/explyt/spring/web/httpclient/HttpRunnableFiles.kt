/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.httpclient

import com.intellij.openapi.vfs.VirtualFile

internal val VirtualFile.hasNioPath: Boolean
    get() = fileSystem.getNioPath(this) != null
