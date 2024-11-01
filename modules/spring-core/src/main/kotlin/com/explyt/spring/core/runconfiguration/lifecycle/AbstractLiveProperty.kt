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
