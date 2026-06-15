/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers

import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider

object JavaSoftAllowDollarClassReferenceProvider : JavaClassReferenceProvider() {
    init {
        setOption(ALLOW_DOLLAR_NAMES, true)
    }

    override fun isSoft() = true
}