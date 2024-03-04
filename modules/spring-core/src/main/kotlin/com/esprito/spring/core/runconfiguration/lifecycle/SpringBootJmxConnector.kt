package com.esprito.spring.core.runconfiguration.lifecycle

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
