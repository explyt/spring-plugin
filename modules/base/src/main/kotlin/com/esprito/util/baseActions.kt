package com.esprito.util

import com.intellij.openapi.application.NonBlockingReadAction
import com.intellij.openapi.application.ReadAction
import java.util.concurrent.Callable

fun <T> nonBlocking(callable: () -> T): NonBlockingReadAction<T> {
    return ReadAction.nonBlocking((Callable { callable() }))
}

fun <T, S> NonBlockingReadAction<T>.then(function: (T) -> S): NonBlockingReadAction<S> {
    return nonBlocking {
        val value = this.executeSynchronously()
        function(value)
    }
}


inline fun <T> runReadNonBlocking(crossinline runnable: () -> T): T {
    return nonBlocking{runnable()}.executeSynchronously()
}