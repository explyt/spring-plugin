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

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import kotlin.concurrent.Volatile

abstract class AsyncLiveProperty<T> protected constructor(
    private val errorHandler: LifecycleErrorHandler, parent: Disposable?, defaultValue: T? = null
) : AbstractLiveProperty<T>(defaultValue) {

    @Volatile
    protected var isDisposed: Boolean = false
        private set

    init {
        if (parent != null) {
            Disposer.register(parent, this)
        }
    }

    override fun retrieve() {
        if (this.isDisposed) return

        ApplicationManager.getApplication().executeOnPooledThread {
            if (this.isDisposed) return@executeOnPooledThread
            try {
                val value = compute()
                val oldValue = setValue(value)
                if (oldValue != value) {
                    broadCastSuccess()
                }
            } catch (ex: LifecycleException) {
                LOG.debug(ex.cause)
                ex.message?.let {
                    errorHandler.handleLifecycleError(it)
                }

                this.setValue(null)
                ex.cause?.let {
                    broadCastFail(it)
                }
            }
        }
    }

    override fun dispose() {
        this.isDisposed = true
    }

    @Throws(LifecycleException::class)
    protected abstract fun compute(): T

    companion object {
        protected val LOG: Logger = Logger.getInstance(
            AsyncLiveProperty::class.java
        )
    }

}
