package com

import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict

@CacheConfig
interface CacheOnInterface {
    @CacheEvict
    fun someMethod()
}
