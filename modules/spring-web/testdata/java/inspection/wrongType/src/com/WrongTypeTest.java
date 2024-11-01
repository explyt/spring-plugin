package com.example.springcoredemo.web;

import org.springframework.web.reactive.function.client.WebClient;
import java.util.UUID;

class UriParametersTest {
    private WebClient webClient;

    void productOk() {
        webClient.put().uri("/product").retrieve()
                .bodyToMono(String.class)
        ;
    }

    void productWrong() {
        webClient.put().uri("/product").retrieve()
                .bodyToMono(UUID.class)
        ;
    }

}