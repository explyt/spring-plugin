/*
 * Copyright © 2025 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.web.view.nodes

import com.intellij.pom.Navigatable

interface EndpointNavigable {
    /**
     * Resolves a [Navigatable] for this node. Must be called inside a read action
     * because it touches PSI. The returned [Navigatable.navigate] call must be
     * performed outside the read action because the platform now requires a
     * write-intent read action when opening file editors
     *
     * Since 253
     */
    fun asNavigatable(): Navigatable?
}
