/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.statistic

import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters
import com.intellij.util.xmlb.XmlSerializer
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticStateRoundTripTest {

    @Test
    fun countersSurviveSerializeThenDeserialize() {
        val original = StatisticState.UsagesMapStatistic()
        original.incrementUsage("FEEDBACK_NUDGE_SHOWN")
        original.incrementUsage("FEEDBACK_NUDGE_STAR_CLICKED")
        original.incrementUsage("FEEDBACK_NUDGE_STAR_CLICKED")

        val element = XmlSerializer.serialize(original, SkipDefaultValuesSerializationFilters())
        val xml = JDOMUtil.writeElement(element)

        val restored = XmlSerializer.deserialize(JDOMUtil.load(xml), StatisticState.UsagesMapStatistic::class.java)

        assertEquals(xml, mapOf("FEEDBACK_NUDGE_SHOWN" to 1, "FEEDBACK_NUDGE_STAR_CLICKED" to 2), restored.counterUsagesMap)
    }

    @Test
    fun theOptionsFileWrittenByTheIdeDeserializesToTheSameCounters() {
        // Verbatim content of config/options/explyt-spring-statistic.xml written by the sandbox IDE (round 7).
        val componentElement = JDOMUtil.load(
            """
            <component name="ExplytSpringStatisticCache">
              <option name="counterUsagesMap">
                <map>
                  <entry key="FEEDBACK_NUDGE_SHOWN" value="1" />
                </map>
              </option>
            </component>
            """.trimIndent()
        )

        val restored = XmlSerializer.deserialize(componentElement, StatisticState.UsagesMapStatistic::class.java)

        assertEquals(mapOf("FEEDBACK_NUDGE_SHOWN" to 1), restored.counterUsagesMap)
    }
}
