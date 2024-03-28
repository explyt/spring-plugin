package com.esprito.spring.core.runconfiguration.clients

import com.fasterxml.jackson.core.JsonFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.jackson.obj

object EspritoLicenseClient {
    private const val LICENSE_SERVICE_URL = "http://localhost:8189"
    private const val VERIFY_URL = "/license/verify"
    private const val TIMEOUT = 5000

    private val gson: Gson by lazy {
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
    }

    fun requestSign(pem: String): Map<String, Any> {
        return HttpRequests.post(LICENSE_SERVICE_URL + VERIFY_URL, HttpRequests.JSON_CONTENT_TYPE)
            .connectTimeout(TIMEOUT)
            .readTimeout(TIMEOUT)
            .accept(HttpRequests.JSON_CONTENT_TYPE)
            .connect { request ->
                val output = BufferExposingByteArrayOutputStream()
                JsonFactory().createGenerator(output).useDefaultPrettyPrinter().use {
                    it.obj {
                        it.writeStringField("pem", pem)
                    }
                }
                request.write(output.toByteArray())
                gson.fromJson(request.reader, object : TypeToken<Map<String, Any?>?>() {}.type)
            }
    }

}