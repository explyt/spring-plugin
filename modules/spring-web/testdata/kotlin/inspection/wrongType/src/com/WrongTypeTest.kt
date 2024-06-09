package com.example.springcoredemo.web

import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID
import java.util.Optional
import java.lang.String

import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.awaitBody

internal class UriParametersTest {
    private val webClient: WebClient? = null

    fun simpleOk() {
        webClient?.put()
            ?.uri("/product/simple")
            ?.retrieve()
            ?.bodyToMono<String>()
    }

    fun simpleWrong() {
        webClient?.put()
            ?.uri("/product/simple")
            ?.retrieve()
            ?.bodyToMono<UUID>()
    }

    fun awaitFlowOk() {
        webClient?.get()
            ?.uri("/product/awaitFlow")
            ?.retrieve()
            ?.awaitBody<Optional<String>>()
    }

    fun awaitFlowWrong() {
        webClient?.get()
            ?.uri("/product/awaitFlow")
            ?.retrieve()
            ?.awaitBody<Optional<UUID>>()
    }

}