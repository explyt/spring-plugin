/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.quarkus.core

import com.intellij.openapi.util.IconLoader

object QuarkusCoreIcons {
    private fun load(path: String) = IconLoader.getIcon(path, QuarkusCoreIcons.javaClass)

    val Advice = load("com/explyt/quarkus/aop/icons/abstractAdvice.svg")
    val Bean = load("com/explyt/quarkus/aop/icons/bean.svg")
    val BeanDependencies = load("com/explyt/quarkus/aop/icons/beanDependencies.svg")
}