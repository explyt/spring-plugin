/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.nodes

interface EndpointNavigable {
    /**
     * Performs navigation to the underlying PSI element.
     *
     * Implementations access PSI and open a file editor, both of which require
     * the EDT under a write-intent read action. Callers must invoke this from
     * the EDT (e.g. via [com.intellij.openapi.application.Application.invokeLater]),
     * not from a plain read action — opening an editor cannot be performed
     * from inside a `ReadAction` ("WriteIntentReadAction can not be called
     * from ReadAction", since 253).
     */
    fun navigate()
}
