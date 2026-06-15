/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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