package com

import org.springframework.beans.factory.annotation.Lookup
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Configuration
open class LookupConfiguration {
    @Lookup
    fun someName(): LookBeanA? {
        return null
    }

    @Lookup("lookBeanA")
    fun lookBeenA(): LookBeanA? {
        return null
    }

    @Lookup("lookBeanA")
    fun lookBeanB(): LookBeanB? {
        return null
    }

    @Lookup("unknown")
    fun unknown(): LookBeanB? {
        return null
    }
}

@Component
class LookBeanA

@Component
class LookBeanB