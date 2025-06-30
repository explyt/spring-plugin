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

package com.explyt.quarkus.core

object QuarkusCoreClasses {
    const val NORMAL_SCOPE = "jakarta.enterprise.context.NormalScope"
    const val SCOPE = "jakarta.inject.Scope"
    const val PATH = "jakarta.ws.rs.Path"
    const val PRODUCES = "jakarta.enterprise.inject.Produces"
    const val INJECT = "jakarta.inject.Inject"
    const val INSTANCE = "jakarta.enterprise.inject.Instance"
    const val ALL = "io.quarkus.arc.All"
    const val DEFAULT_BEAN = "io.quarkus.arc.DefaultBean"

    const val DECORATOR = "jakarta.decorator.Decorator"
    const val DELEGATE = "jakarta.decorator.Delegate"

    const val INTERCEPTOR = "jakarta.interceptor.Interceptor"
    const val INTERCEPTOR_BINDING = "jakarta.interceptor.InterceptorBinding"

    const val CORE_CLASS = "io.quarkus.runtime.configuration.AbstractConfigBuilder"

    val COMPONENTS_ANNO = setOf(NORMAL_SCOPE, SCOPE, PATH, INTERCEPTOR, DECORATOR)
}