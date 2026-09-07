/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.statistic

import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.SkipDefaultsSerializationFilter
import com.intellij.util.xmlb.XmlSerializer
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticStateSerializationTest {

    @Test
    fun populatedCountersSerializeToXmlThePlatformCanPersist() {
        val state = StatisticState.UsagesMapStatistic()
        state.incrementUsage("FEEDBACK_NUDGE_SHOWN")
        state.incrementUsage("FEEDBACK_NUDGE_STAR_CLICKED")
        state.incrementUsage("FEEDBACK_NUDGE_STAR_CLICKED")

        val element = XmlSerializer.serialize(state, SkipDefaultsSerializationFilter())
        assertNotNull("a populated state must not serialize to nothing", element)
        val xml = JDOMUtil.writeElement(element)
        println(xml)
        assertTrue(xml, xml.contains("FEEDBACK_NUDGE_SHOWN"))
        assertTrue(xml, xml.contains("FEEDBACK_NUDGE_STAR_CLICKED"))
    }
}
