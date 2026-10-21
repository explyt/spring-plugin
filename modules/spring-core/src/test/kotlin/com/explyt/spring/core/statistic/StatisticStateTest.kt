/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.statistic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticStateTest {

    @Test
    fun incrementingAUsageMarksTheStateModifiedSoThePlatformPersistsIt() {
        val state = StatisticState.UsagesMapStatistic()
        val before = state.modificationCount

        state.incrementUsage("GUTTER_BEAN_USAGE")
        state.incrementUsage("GUTTER_BEAN_USAGE")
        state.incrementUsage("SETTINGS_OPEN")

        assertEquals(2, state.counterUsagesMap["GUTTER_BEAN_USAGE"])
        assertEquals(1, state.counterUsagesMap["SETTINGS_OPEN"])
        assertTrue(
            "modificationCount must grow, otherwise the component is never saved",
            state.modificationCount > before,
        )
    }
}
