package com.app

import org.springframework.beans.factory.dsl.BeanRegistrarDsl

class MyBeanRegistrar : BeanRegistrarDsl({
    registerBean<Foo>()
    registerBean<Bar>(name = "bar")
    profile("baz") {
        registerBean { Baz("Hello World!") }
    }
})
