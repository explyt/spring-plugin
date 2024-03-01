package com.example.springcoredemo.web

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

class TemplateParametersTest {

    fun tooFew() {
        MockMvcRequestBuilders.put("/product/{tooFew}")
    }

    fun tooMany() {
        MockMvcRequestBuilders.get("/product/{tooMany}", 1, 2)
    }

    fun exact() {
        MockMvcRequestBuilders.put("/product/{exact}", 1)
    }

    fun tooFewConst() {
        MockMvcRequestBuilders.put(TOO_FEW_CONST);
    }

    companion object {
        const val TOO_FEW_CONST = "/product/{tooFewConst}"
    }

}
