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

package com.explyt.spring.web

import com.explyt.util.MultiVendorClass
import kotlin.reflect.KProperty

object WebEeClasses {
    val JAX_RS_APPLICATION_PATH by "ws.rs.ApplicationPath"
    val JAX_RS_PATH by "ws.rs.Path"
    val JAX_RS_HTTP_METHOD by "ws.rs.HttpMethod"
    val JAX_RS_PRODUCES by "ws.rs.Produces"
    val JAX_RS_CONSUMES by "ws.rs.Consumes"
    val JAX_RS_PATH_PARAM by "ws.rs.PathParam"
    val JAX_RS_QUERY_PARAM by "ws.rs.QueryParam"
    val JAX_RS_HEADER_PARAM by "ws.rs.HeaderParam"
    val JAX_RS_DEFAULT by "ws.rs.DefaultValue"

    private operator fun String.getValue(jpaClasses: WebEeClasses, property: KProperty<*>): MultiVendorClass {
        return MultiVendorClass(this)
    }
}