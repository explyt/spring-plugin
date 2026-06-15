/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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

    val AROUNT_TIMEOUT by "interceptor.AroundTimeout"
    val AROUNT_INVOKE by "interceptor.AroundInvoke"
    val AROUNT_CONSTRUCT by "interceptor.AroundConstruct"

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