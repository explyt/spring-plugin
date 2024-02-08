package com.event.listener

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class TestEventListenerWithRecursive {
    @EventListener
    annotation class CustomEventListener

    @CustomEventListener
    fun test0() {
    }

    @CustomEventListener
    protected fun test1() {
    }

    @CustomEventListener
    private fun test2() {
    }

    @CustomEventListener
    fun test3() {
    }

    @CustomEventListener
    fun test4(param1: String?, param2: String?) {
    }

    @CustomEventListener
    private fun test5(param1: String, param2: String) {
    }

    @CustomEventListener
    fun test6(param1: String?) {
    }
}