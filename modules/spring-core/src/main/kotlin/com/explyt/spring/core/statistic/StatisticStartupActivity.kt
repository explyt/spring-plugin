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