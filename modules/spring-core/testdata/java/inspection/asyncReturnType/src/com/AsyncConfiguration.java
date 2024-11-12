package com;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

@Configuration
@Async
public class AsyncConfiguration {
    public void validReturnTypeVoid() {
    }
    
    public CompletableFuture<String> validReturnTypeFuture() {
        return null
    }
    
    public String invalidReturnType() {
        return "";
    }
}