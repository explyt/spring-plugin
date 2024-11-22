package com.example.springcoredemo.web

import java.lang.String
import java.util.Optional
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

import kotlinx.coroutines.flow.Flow

@RestController
class WrongTypeController {
    private val service: WrongTypeService? = null

    @PutMapping(value = ["/product/simple"])
    @ResponseBody
    fun simple(): Mono<String> {
        return service?.simple()
    }

    @GetMapping(value = ["/product/awaitFlow"])
    @ResponseBody
    fun flow(): Flow<Optional<String>> {
        return service?.flow()
    }

}