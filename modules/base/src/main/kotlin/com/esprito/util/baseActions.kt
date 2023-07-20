package com.esprito.util

import com.intellij.openapi.application.NonBlockingReadAction
import com.intellij.openapi.application.ReadAction
import java.util.concurrent.Callable



fun nonBlocking(runnable: Runnable): NonBlockingReadAction<Boolean> {
    return ReadAction.nonBlocking((Callable {
        runnable.run()
        true
    }))
}

fun <T> nonBlocking(callable: () -> T): NonBlockingReadAction<T> {
    return ReadAction.nonBlocking((Callable { callable() }))
}

fun <T, S> NonBlockingReadAction<T>.then(function: (T) -> S): NonBlockingReadAction<S> {
    return nonBlocking {
        val value = this.executeSynchronously()
        function(value)
    }
}

fun <S> NonBlockingReadAction<Boolean?>.thenIfTrue(callable: () -> S): NonBlockingReadAction<S?> {
    return nonBlocking {
        if (this.executeSynchronously() == true) {
            callable()
        } else {
            null
        }
    }
}

inline fun <T> runReadNonBlocking(crossinline runnable: () -> T): T {
    return nonBlocking{runnable()}.executeSynchronously()
}