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

import com.explyt.spring.core.runconfiguration.SpringRunConfigurationDetectService
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.io.path.exists

private const val STATISTIC_PREFIX = "explyt-spring-statistic"

@Service(Service.Level.APP)
class StatisticService {
    private val logger = Logger.getInstance(StatisticService::class.java)

    fun addActionUsage(actionId: StatisticActionId) {
        if (!SpringToolRunConfigurationsSettingsState.getInstance().isCollectStatistic) return
        if (skipForUnitTestAndHeadlessMode()) return

        try {
            synchronized(StatisticService::class.java) {
                val statisticState = getStatisticState()
                statisticState.state.counterUsagesMap.compute(actionId.name) { _, count ->
                    if (count == null) 1 else count + 1
                }
            }
        } catch (e: Exception) {
            logger.warn(e)
        }
    }

    fun writeStateToFile() {
        try {
            val localDate = LocalDate.now()
            val lockFilePath = getStatisticDir().toPath().resolve("$localDate.lock")
            createLockFile(lockFilePath) ?: return
            try {
                writeStateToFile(localDate)
            } finally {
                FileUtil.delete(lockFilePath)
            }
        } catch (e: Exception) {
            logger.warn(e)
        }
    }

    fun removeOldFile() {
        try {
            val dateToRemove = OffsetDateTime.now().minusMonths(1).toInstant()
            val listFiles = getStatisticDir().listFiles() ?: emptyArray()
            listFiles
                .filter { Files.getLastModifiedTime(it.toPath()).toInstant() < dateToRemove }
                .forEach { FileUtil.delete(it) }

        } catch (e: Exception) {
            logger.warn(e)
        }
    }

    private fun writeStateToFile(localDate: LocalDate) {
        val descriptor = pluginDescriptor() ?: return
        if (skipForNonProductionMode(descriptor)) {
            clearStatistic()
            return
        }
        if (!SpringToolRunConfigurationsSettingsState.getInstance().isCollectStatistic) return

        val deviceId = getDeviceId() ?: return

        val fileName = "$STATISTIC_PREFIX-$localDate.properties"
        val resultDailyPath = getStatisticDir().toPath().resolve(fileName)
        if (!checkLastUpdateDate(resultDailyPath)) return

        val counterMap = getCurrentStatistic().takeIf { it.isNotEmpty() } ?: return
        val properties = getCurrentFilePropertiesState(resultDailyPath)
        updateStatistic(properties, deviceId, descriptor, localDate, counterMap)
        saveStatisticToFile(resultDailyPath, properties, fileName)
    }

    private fun updateStatistic(
        properties: Properties,
        deviceId: String,
        descriptor: PluginDescriptor,
        localDate: LocalDate,
        counterMap: Map<String, Int>
    ) {
        properties[StatisticActionId.DEVICE_ID.name] = deviceId
        properties[StatisticActionId.PLUGIN_VERSION.name] = descriptor.version
        properties[StatisticActionId.LOCAL_DATE.name] = localDate.toString()
        for (entry in counterMap) {
            val value = properties[entry.key] as? String
            val totalValue = value?.toIntOrNull()?.let { it + entry.value } ?: entry.value
            properties[entry.key] = totalValue.toString()
        }
    }

    private fun saveStatisticToFile(resultDailyPath: Path, properties: Properties, fileName: String) {
        FileOutputStream(resultDailyPath.toFile()).use {
            properties.store(it, fileName)
        }
    }

    private fun getCurrentFilePropertiesState(resultDailyPath: Path): Properties {
        if (!resultDailyPath.exists()) return Properties()
        return try {
            val properties = Properties()
            val fileInputStream = FileInputStream(resultDailyPath.toFile())
            fileInputStream.use { properties.load(fileInputStream) }
            return properties
        } catch (e: Exception) {
            logger.warn(e)
            Properties()
        }
    }

    private fun getCurrentStatistic(): Map<String, Int> {
        try {
            synchronized(StatisticService::class.java) {
                val counterUsagesMap = getStatisticState().state.counterUsagesMap
                val result = HashMap(counterUsagesMap)
                getStatisticState().state.counterUsagesMap.clear()
                return result
            }
        } catch (e: Exception) {
            logger.warn(e)
            return emptyMap()
        }
    }

    private fun clearStatistic() = getStatisticState().state.counterUsagesMap.clear()


    private fun getStatisticState(): StatisticState =
        ApplicationManager.getApplication().getService(StatisticState::class.java)


    private fun skipForNonProductionMode(pluginDescriptor: PluginDescriptor): Boolean {
        if (skipForUnitTestAndHeadlessMode()) return true

        if (Registry.`is`("explyt.statistic.debug")) return false

        return pluginDescriptor.version.contains("snapshot", true)
    }

    fun skipForUnitTestAndHeadlessMode() = (ApplicationManager.getApplication().isUnitTestMode
            || ApplicationManager.getApplication().isHeadlessEnvironment)

    //check last update date to prevent frequent file updates
    private fun checkLastUpdateDate(resultDailyPath: Path): Boolean {
        if (Registry.`is`("explyt.statistic.debug")) {
            logger.info("Explyt statistic file path: $resultDailyPath")
            return true
        }

        try {
            val lastModifiedInstant = Files.getLastModifiedTime(resultDailyPath).toInstant()
            val duration = Duration.between(lastModifiedInstant, Instant.now())
            return duration > UPDATE_MIN_INTERVAL
        } catch (e: Exception) {
            return true
        }
    }

    private fun getStatisticDir(): File {
        val directory = File(PathManager.getLogPath(), STATISTIC_PREFIX)
        FileUtil.createDirectory(directory)
        return directory
    }

    private fun getDeviceId(): String? {
        return null
    }

    private fun pluginDescriptor(): PluginDescriptor? {
        return PluginManager.getPluginByClass(SpringRunConfigurationDetectService::class.java)
    }

    private fun createLockFile(lockFilePath: Path): Path? {
        return try {
            Files.createFile(lockFilePath)
        } catch (e: FileAlreadyExistsException) {
            try {
                val lastModifiedInstant = Files.getLastModifiedTime(lockFilePath).toInstant()
                if (OffsetDateTime.now().minusMinutes(10).toInstant() > lastModifiedInstant) {
                    FileUtil.delete(lockFilePath)
                }
            } catch (_: Exception) {
            }
            null
        }
    }

    companion object {
        fun getInstance(): StatisticService = service()
        private val UPDATE_MIN_INTERVAL = Duration.ofMinutes(30)
    }
}