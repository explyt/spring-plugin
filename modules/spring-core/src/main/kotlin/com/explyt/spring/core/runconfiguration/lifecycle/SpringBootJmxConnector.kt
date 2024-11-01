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

import java.io.Closeable
import java.io.IOException
import javax.management.MBeanServerConnection
import javax.management.MalformedObjectNameException
import javax.management.ObjectName
import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL

open class SpringBootJmxConnector(private val serviceUrl: String, objectName: String) : Closeable {
    protected val objectName: ObjectName = toObjectName(objectName)
    private var connector: JMXConnector? = null
    private var connection: MBeanServerConnection? = null

    @get:Throws(IOException::class)
    private val jmxConnector: JMXConnector
        get() = JMXConnectorFactory.connect(JMXServiceURL(serviceUrl), null)

    @get:Throws(IOException::class)
    protected val jmxConnection: MBeanServerConnection?
        get() {
            if (connection == null) {
                if (connector == null) {
                    connector = jmxConnector
                }

                connection = connector?.mBeanServerConnection
            }

            return connection
        }

    override fun close() {
        try {
            connector?.close()
        } catch (_: IOException) {
        }

        connector = null
    }

    companion object {
        private fun toObjectName(objectName: String): ObjectName {
            try {
                return ObjectName(objectName)
            } catch (ex: MalformedObjectNameException) {
                throw IllegalArgumentException("Invalid JMX object name '$objectName'")
            }
        }
    }
}
