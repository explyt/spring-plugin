/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
import junit.framework.TestCase

private const val TEST_DATA_PATH = "loader/src"

class SpringWebFeignClientLoaderTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springCloud_4_1_3
    )

    fun testFeignClientLoader_param_url_path() {
        myFixture.configureByFiles("OrderFeignClient.java")
        myFixture.doHighlighting()

        val endpoints = SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project)
            .flatMapTo(mutableListOf()) { it.searchEndpoints(module) }

        assertEquals(1, endpoints.size)

        TestCase.assertEquals(
            "https://market.com/v1/order",
            endpoints.map { it.path }
                .firstOrNull()
        )
    }

    fun testFeignClientLoader_property_url() {
        myFixture.configureByFiles("OrganizationFeignClient.java", "application.yaml")
        myFixture.doHighlighting()

        val endpoints = SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project)
            .flatMapTo(mutableListOf()) { it.searchEndpoints(module) }

        assertEquals(1, endpoints.size)

        TestCase.assertEquals(
            "https://organization.com/get",
            endpoints.map { it.path }
                .firstOrNull()
        )
    }

    fun testFeignClientLoader_only_property_url() {
        myFixture.configureByFiles("ProductFeignClient.java", "application.yaml")
        myFixture.doHighlighting()

        val endpoints = SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project)
            .flatMapTo(mutableListOf()) { it.searchEndpoints(module) }

        assertEquals(1, endpoints.size)

        TestCase.assertEquals(
            "https://order-service.com/product",
            endpoints.map { it.path }
                .firstOrNull()
        )
    }
}