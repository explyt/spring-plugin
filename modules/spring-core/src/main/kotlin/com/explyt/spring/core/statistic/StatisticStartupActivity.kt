/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.statistic

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.registry.Registry
import kotlinx.coroutines.delay
import java.util.concurrent.ThreadLocalRandom
import kotlin.time.Duration.Companion.seconds

class StatisticStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        if (StatisticService.getInstance().skipForUnitTestAndHeadlessMode()) return

        val initDelaySec = ThreadLocalRandom.current().nextLong(60) // initial delay from 0 to 60 seconds
        val intervalSec = Registry.intValue("explyt.statistic.interval", 3600)

        delay(initDelaySec.seconds)

        StatisticService.getInstance().removeOldFile()
        while (!ApplicationManager.getApplication().isUnitTestMode) {
            StatisticService.getInstance().writeStateToFile()
            delay(intervalSec.seconds)
        }
    }
}