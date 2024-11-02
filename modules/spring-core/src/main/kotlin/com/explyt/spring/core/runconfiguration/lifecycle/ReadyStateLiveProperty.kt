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

package com.explyt.spring.core.runconfiguration.lifecycle

import com.intellij.concurrency.JobScheduler
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import java.util.concurrent.TimeUnit
import kotlin.concurrent.Volatile

class ReadyStateLiveProperty(
    private val moduleDescriptor: LiveProperty<SpringBootModuleDescriptor>,
    private val serviceUrl: JmxServiceUrlLiveProperty,
    private val errorHandler: LifecycleErrorHandler,
    parent: Disposable?
) : AbstractLiveProperty<Boolean>(false) {
    private val lock = Any()
    private var checker: Runnable? = null
    private var start: Long = 0
    private var retryCount: Long = 0

    @Volatile
    private var isDisposed = false

    override fun dispose() {
        isDisposed = true
    }

    override fun retrieve() {
        synchronized(lock) {
            start = System.currentTimeMillis()
            retryCount = 0L
        }

        val scheduler = JobScheduler.getScheduler()
        scheduler.schedule(serviceUrl::retrieve, STATE_CHECKING_DELAY_INTERVAL, TimeUnit.MILLISECONDS)
    }

    private fun startChecking() {
        synchronized(lock) {
            if (checker != null) return

            setValue(false)
            checker = object : Runnable {
                override fun run() {
                    if (isDisposed || value == true) return

                    if (isTimeoutExceeded()) {
                        broadCastFail(LifecycleException("Application ready check timeout", null))
                        return
                    }

                    val moduleDescriptorValue = moduleDescriptor.value
                    val serviceUrlValue = serviceUrl.value

                    if (serviceUrlValue != null && moduleDescriptorValue != null) {
                        try {
                            SpringBootApplicationConnector(
                                serviceUrlValue, moduleDescriptorValue
                            ).use { connector ->
                                if (connector.isReady) {
                                    setValue(true)
                                    broadCastSuccess()
                                }
                            }

                        } catch (ex: Exception) {
                            if (isRetryCountExceeded()) {
                                LOG.debug(ex)
                                broadCastFail(ex)
                                return
                            }
                        }

                    }

                    JobScheduler.getScheduler()
                        .schedule(this, STATE_CHECKING_RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                }

                private fun isTimeoutExceeded(): Boolean {
                    synchronized(lock) {
                        if (System.currentTimeMillis() - start > STATE_CHECKING_TIMEOUT) {
                            setValue(null)
                            checker = null
                            return true
                        }
                        return false
                    }
                }

                private fun isRetryCountExceeded(): Boolean {
                    synchronized(lock) {
                        ++retryCount
                        if (retryCount >= STATE_CHECKING_RETRY_COUNT) {
                            setValue(null)
                            checker = null
                            return true
                        }
                        return false
                    }
                }
            }

            checker?.let {
                ApplicationManager.getApplication().executeOnPooledThread(it)
            }
        }
    }

    init {
        serviceUrl.addListener(object : LiveProperty.PropertyListener {
            override fun onFinish() {
                if (!StringUtil.isEmpty(serviceUrl.value)) {
                    startChecking()
                }
            }

            override fun onFail(ex: Exception) {
                if (ex is LifecycleException) {
                    synchronized(lock) {
                        ++retryCount
                        if (retryCount < STATE_CHECKING_RETRY_COUNT && System.currentTimeMillis() - start <= SERVICE_URL_RETRIEVING_TIMEOUT) {
                            val scheduler = JobScheduler.getScheduler()
                            scheduler.schedule(
                                serviceUrl::retrieve, STATE_CHECKING_RETRY_INTERVAL, TimeUnit.MILLISECONDS
                            )
                            return
                        }
                    }

                    if (retryCount >= STATE_CHECKING_RETRY_COUNT) {
                        errorHandler.handleLifecycleError("Failed to retrieve jmx service")
                    }
                }

                setValue(null)
                broadCastFail(ex)
            }
        })
        if (parent != null) {
            Disposer.register(parent, this)
        }
    }

    companion object {
        private val LOG = Logger.getInstance(ReadyStateLiveProperty::class.java)
        private const val STATE_CHECKING_RETRY_INTERVAL = 500L
        private const val STATE_CHECKING_DELAY_INTERVAL = 1000L
        private val STATE_CHECKING_TIMEOUT = TimeUnit.MINUTES.toMillis(5L)
        private val SERVICE_URL_RETRIEVING_TIMEOUT = TimeUnit.MINUTES.toMillis(1L)
        private const val STATE_CHECKING_RETRY_COUNT = 20
    }
}
