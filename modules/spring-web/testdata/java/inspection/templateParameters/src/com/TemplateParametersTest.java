package com.example.springcoredemo.web;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

class TemplateParametersTest {
    
    void tooFew() {
        MockMvcRequestBuilders.put("/product/{tooFew}");
    }
    
    void tooMany() {
        MockMvcRequestBuilders.get("/product/{tooMany}", 1, 2);
    }
    
    void exact() {
        MockMvcRequestBuilders.put("/product/{exact}", 1);
    }
    
    void tooFewConst() {
        final String tooFewConst = "/product/{tooFewConst}";
        MockMvcRequestBuilders.put(tooFewConst);
    }
    
}