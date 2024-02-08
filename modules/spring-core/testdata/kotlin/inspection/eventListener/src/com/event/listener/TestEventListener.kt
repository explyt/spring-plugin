package com.event.listener

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class TestEventListener {
    @EventListener
    fun test0() {
    }

    @EventListener
    protected fun test1() {
    }

    @EventListener
    private fun test2() {
    }

    @EventListener
    fun test3() {
    }

    @EventListener
    fun test4(param1: String?, param2: String?) {
    }

    @EventListener
    private fun test5(param1: String, param2: String) {
    }

    @EventListener
    fun test6(param1: String?) {
    }
}