package com;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;

@CacheConfig
public interface CacheOnInterface {
    @CacheEvict
    void someMethod();
}