/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.action

import org.junit.Assert
import org.junit.Test

/**
 * `ToolWindowManager.getToolWindow` matches the registered `id` exactly and returns `null` for anything else, so a
 * mismatch between the constant the attach action activates and the `id` declared in `spring-core-plugin.xml` would
 * turn the activation into a silent no-op — the exact symptom issue #197 reported in the first place.
 *
 * Activation itself cannot be asserted in a headless test: `ToolWindowHeadlessManagerImpl.MockToolWindow` hardcodes
 * `isActive()` and `isVisible()` to `false` and its `activate` only runs the callback. The identifier is the part
 * that can silently drift, so it is the part pinned here.
 */
class AttachSpringBootProjectActionToolWindowIdTest {

    @Test
    fun testActivatedIdMatchesTheRegisteredToolWindow() {
        val descriptor = javaClass.getResource("/META-INF/spring-core-plugin.xml")
            ?: Assert.fail("spring-core-plugin.xml is not on the test classpath").let { error("unreachable") }
        val declaredIds = TOOL_WINDOW_ID_REGEX.findAll(descriptor.readText())
            .map { it.groupValues[1] }
            .toList()

        Assert.assertTrue(
            "spring-core-plugin.xml declares no toolWindow; the descriptor or this test is out of date",
            declaredIds.isNotEmpty()
        )
        Assert.assertTrue(
            "the attach action activates '${AttachSpringBootProjectAction.TOOL_WINDOW_ID}', " +
                    "which no toolWindow in spring-core-plugin.xml declares: $declaredIds",
            AttachSpringBootProjectAction.TOOL_WINDOW_ID in declaredIds
        )
    }

    private companion object {
        val TOOL_WINDOW_ID_REGEX = Regex("""<toolWindow\s+id="([^"]+)"""")
    }
}
