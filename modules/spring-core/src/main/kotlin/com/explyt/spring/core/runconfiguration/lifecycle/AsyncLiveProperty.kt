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
