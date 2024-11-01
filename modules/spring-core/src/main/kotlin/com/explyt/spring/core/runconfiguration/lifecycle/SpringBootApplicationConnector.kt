/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.runconfiguration.lifecycle

import org.jetbrains.kotlin.parsing.parseBoolean
import java.util.*
import javax.management.InstanceNotFoundException

class SpringBootApplicationConnector(serviceUrl: String, moduleDescriptor: SpringBootModuleDescriptor) :
    SpringBootJmxConnector(serviceUrl, moduleDescriptor.applicationAdminJmxName) {

    @Throws(Exception::class)
    fun getProperty(propertyName: String): Any? = this.doGetProperty(propertyName)

    @Throws(Exception::class)
    fun getBooleanProperty(propertyName: String, defaultValue: Boolean): Boolean {
        return when (val property = getProperty(propertyName)) {
            is Boolean -> property
            is String -> parseBoolean(property)
            else -> defaultValue
        }
    }


    @get:Throws(Exception::class)
    val serverPort: Int
        get() {
            val value = this.getProperty(LOCAL_SERVER_PORT_PROPERTY)
            return if (value is Int) {
                value
            } else {
                (value as? String)?.toIntOrNull() ?: -1
            }
        }

    @get:Throws(Exception::class)
    val isReady: Boolean
        get() {
            return try {
                this.jmxConnection?.getAttribute(this.objectName, READY_ATTRIBUTE) as? Boolean ?: false
            } catch (var2: InstanceNotFoundException) {
                false
            }
        }

    @Throws(Exception::class)
    private fun doGetProperty(propertyName: String): Any? {
        return this.jmxConnection?.invoke(
            this.objectName, GET_PROPERTY_OPERATION, arrayOf(propertyName), arrayOf(String::class.java.name)
        )
    }

    companion object {
        private const val READY_ATTRIBUTE = "Ready"
        private const val GET_PROPERTY_OPERATION = "getProperty"
        private const val LOCAL_SERVER_PORT_PROPERTY = "local.server.port"
        const val SERVER_SSL_KEY_STORE = "server.ssl.key-store"
        const val SERVER_SSL_ENABLED = "server.ssl.enabled"
        const val SERVER_ADDRESS = "server.address"
        const val SERVER_CONTEXT_PATH_PROPERTY = "server.servlet.context-path"
        const val SERVER_SERVLET_PATH_PROPERTY = "spring.mvc.servlet.path"
    }
}
