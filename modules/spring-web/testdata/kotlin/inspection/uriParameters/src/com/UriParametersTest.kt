package com.example.springcoredemo.web

import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClient

class UriParametersTest {
    private val webClient: WebClient? = null
    private val webTestClient: WebTestClient? = null

    fun tooFew() {
        webClient.put().uri("/product/{tooFew}")
        webTestClient.put().uri("/product/{tooFew}")
    }

    fun tooMany() {
        webClient.put().uri("/product/{tooMany}", 1, 2)
        webTestClient.put().uri("/product/{tooMany}", 1, 2)
    }

    fun exact() {
        webClient.put().uri("/product/{exact}", 1)
        webTestClient.put().uri("/product/{exact}", 1)
    }

    fun tooFewConst() {
        webClient.put().uri(TOO_FEW_CONST)
        webTestClient.put().uri(TOO_FEW_CONST)
    }

    companion object {
        const val TOO_FEW_CONST = "/product/{tooFewConst}"
    }

}
