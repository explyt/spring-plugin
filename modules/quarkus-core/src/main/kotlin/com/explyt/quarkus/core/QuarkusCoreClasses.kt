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

import com.explyt.util.MultiVendorClass
import kotlin.reflect.KProperty

object QuarkusCoreClasses {
    val NORMAL_SCOPE by "enterprise.context.NormalScope"
    val SCOPE by "inject.Scope"
    val PATH by "ws.rs.Path"
    val PRODUCES by "enterprise.inject.Produces"
    val INJECT by "inject.Inject"
    val INSTANCE by "enterprise.inject.Instance"

    val DECORATOR by "decorator.Decorator"
    val DELEGATE by "decorator.Delegate"

    val INTERCEPTOR by "interceptor.Interceptor"
    val INTERCEPTOR_BINDING by "interceptor.InterceptorBinding"
    val HTTP_METHOD by "ws.rs.HttpMethod"

    const val ALL = "io.quarkus.arc.All"
    const val DEFAULT_BEAN = "io.quarkus.arc.DefaultBean"
    const val CORE_CLASS = "io.quarkus.runtime.configuration.AbstractConfigBuilder"

    val COMPONENTS_ANNO = (NORMAL_SCOPE.allFqns + SCOPE.allFqns + PATH.allFqns + INTERCEPTOR.allFqns + DECORATOR.allFqns).toSet()

    private operator fun String.getValue(jpaClasses: QuarkusCoreClasses, property: KProperty<*>): MultiVendorClass {
        return MultiVendorClass(this)
    }
}