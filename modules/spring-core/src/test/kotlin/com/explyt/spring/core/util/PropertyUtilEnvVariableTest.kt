/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import org.junit.Assert
import org.junit.Test

class PropertyUtilEnvVariableTest {

    @Test
    fun testSimpleKey() {
        Assert.assertEquals("SPRING_DATASOURCE_URL", PropertyUtil.toEnvironmentVariableName("spring.datasource.url"))
    }

    @Test
    fun testDashesAreRemoved() {
        Assert.assertEquals(
            "SPRING_MAIN_LOGSTARTUPINFO",
            PropertyUtil.toEnvironmentVariableName("spring.main.log-startup-info")
        )
        Assert.assertEquals(
            "MY_MAINPROJECT_PERSON_FIRSTNAME",
            PropertyUtil.toEnvironmentVariableName("my.main-project.person.first-name")
        )
    }

    @Test
    fun testListIndexIsSurroundedWithUnderscores() {
        Assert.assertEquals("MY_SERVICE_0_OTHER", PropertyUtil.toEnvironmentVariableName("my.service[0].other"))
    }

    @Test
    fun testTrailingListIndex() {
        Assert.assertEquals("MY_LIST_10", PropertyUtil.toEnvironmentVariableName("my.list[10]"))
    }

    @Test
    fun testAlreadyUppercaseKeptStable() {
        Assert.assertEquals("SERVER_PORT", PropertyUtil.toEnvironmentVariableName("SERVER.PORT"))
    }

    @Test
    fun testMapKeyWithDots() {
        Assert.assertEquals(
            "LOGGING_LEVEL_ORG_HIBERNATE_SQL",
            PropertyUtil.toEnvironmentVariableName("logging.level.org.hibernate.SQL")
        )
    }
}
