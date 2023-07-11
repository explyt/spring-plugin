package com.esprito.spring.test

data class TestLibrary(val name: String, val jar: String) {
    companion object {
        val springContext6 = TestLibrary("Gradle org.springframework:spring-context:6.0.7", "spring-context-6.0.7.jar")
    }
}
