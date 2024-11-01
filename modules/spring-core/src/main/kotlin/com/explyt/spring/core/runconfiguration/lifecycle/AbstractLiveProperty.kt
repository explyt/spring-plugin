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

import com.explyt.spring.core.runconfiguration.lifecycle.LiveProperty.PropertyListener
import com.intellij.util.containers.ContainerUtil
import java.util.concurrent.atomic.AtomicReference

abstract class AbstractLiveProperty<T> protected constructor(defaultValue: T? = null) : LiveProperty<T> {
    private val property: AtomicReference<T> = AtomicReference<T>()

    override val value: T?
        get() = property.get()

    private val myListeners: MutableList<PropertyListener> =
        ContainerUtil.createLockFreeCopyOnWriteList()

    init {
        property.set(defaultValue)
    }

    protected open fun setValue(value: T?): T? = property.getAndSet(value)

    protected val listeners: List<PropertyListener>
        get() = this.myListeners

    override fun addListener(listener: PropertyListener) {
        myListeners.add(listener)
    }

    override fun removeListener(listener: PropertyListener) {
        myListeners.remove(listener)
    }

    protected fun broadCastSuccess() {
        listeners.forEach {
            it.changed()
            it.onFinish()
        }
    }

    protected fun broadCastFail(throwable: Throwable) {
        val ex = if (throwable is Exception) throwable else LifecycleException(throwable.message, throwable)

        listeners.forEach {
            it.onFail(ex)
            it.onFinish()
        }
    }

}
