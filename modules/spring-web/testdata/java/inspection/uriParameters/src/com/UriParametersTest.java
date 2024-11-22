package com.example.springcoredemo.web;

import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

class UriParametersTest {
    
    private WebClient webClient;
    private WebTestClient webTestClient;
    
    void tooFew() {
        webClient.put().uri("/product/{tooFew}");
        webTestClient.put().uri("/product/{tooFew}");
    }
    
    void tooMany() {
        webClient.put().uri("/product/{tooMany}", 1, 2);
        webTestClient.put().uri("/product/{tooMany}", 1, 2);
    }
    
    void exact() {
        webClient.put().uri("/product/{exact}", 1);
        webTestClient.put().uri("/product/{exact}", 1);
    }
    
    void tooFewConst() {
        final String tooFewConst = "/product/{tooFewConst}";
        webClient.put().uri(tooFewConst);
        webTestClient.put().uri(tooFewConst);
    }
    
}