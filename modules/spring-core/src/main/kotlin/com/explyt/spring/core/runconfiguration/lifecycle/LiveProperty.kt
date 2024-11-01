package com.explyt.spring.core.runconfiguration.lifecycle

import com.intellij.openapi.Disposable

interface LiveProperty<T> : Disposable {
    val value: T?

    fun retrieve()
    fun addListener(listener: PropertyListener)
    fun removeListener(listener: PropertyListener)

    interface PropertyListener {
        fun changed() {}
        fun onFail(ex: Exception) {}
        fun onFinish() {}
    }
}