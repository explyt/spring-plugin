package com

import org.springframework.cache.annotation.Caching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Async
import org.springframework.transaction.annotation.Transactional
import javax.swing.text.html.parser.Entity

@Async
class GeneratorServiceImpl : GeneratorService {
    override fun generatorService(entity: Entity?) {}

    @Transactional
    fun key() {
    }
}

internal interface GeneratorService {
    fun generatorService(entity: Entity?)
}

open class CachingService {
    @Caching
    fun caching() {
    }
}

@Configuration
open class FinalConfiguration {
    @Bean
    fun getObj(): FinalObject {
        return FinalObject()
    }
}

class FinalObject