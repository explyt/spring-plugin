/*
 * Copyright (c) 2008-2020 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.esprito.jpa.util

import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.AbstractQuery
import com.intellij.util.Processor
import com.intellij.util.Query
import java.util.*

class UniqueResultsQuery<T, M>(
    private val myOriginal: Query<out T>,
    private val myMapper: (T) -> M
) :
    AbstractQuery<T>() {
    override fun processResults(consumer: Processor<in T>): Boolean {
        return delegateProcessResults(myOriginal, MyProcessor(consumer))
    }

    override fun forEach(consumer: Processor<in T>): Boolean {
        return myOriginal.forEach(MyProcessor(consumer))
    }

    private inner class MyProcessor(private val myConsumer: Processor<in T>) : Processor<T> {
        private val myProcessedElements: MutableSet<in M>

        init {
            myProcessedElements = Collections.synchronizedSet(HashSet())
        }

        override fun process(t: T): Boolean {
            ProgressManager.checkCanceled()
            // in case of exception do not mark the element as processed, we couldn't recover otherwise
            val m = myMapper(t)
            if (myProcessedElements.contains(m)) return true
            val result = myConsumer.process(t)
            myProcessedElements.add(m)
            return result
        }
    }

    @Suppress("HardCodedStringLiteral")
    override fun toString(): String {
        return "UniqueQuery: $myOriginal"
    }
}
