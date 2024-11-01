/*
 * Copyright © 2024 Explyt Ltd
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

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
import junit.framework.TestCase

private const val TEST_DATA_PATH = "loader/src"

class SpringWebFeignClientLoaderTest : ExplytKotlinLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springCloud_4_1_3
    )

    fun testFeignClientLoader_param_url_path() {
        myFixture.configureByFiles("OrderFeignClient.kt")
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
        myFixture.configureByFiles("OrganizationFeignClient.kt", "application.properties")
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
        myFixture.configureByFiles("ProductFeignClient.kt", "application.properties")
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