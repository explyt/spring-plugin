/*
 * Copyright © 2025 Explyt Ltd
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

package io.sentry.marshaller.json

import com.fasterxml.jackson.core.JsonGenerator
import io.sentry.event.Breadcrumb
import io.sentry.event.Event
import io.sentry.event.Sdk
import io.sentry.event.interfaces.SentryInterface
import io.sentry.marshaller.Marshaller
import io.sentry.util.Util
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.OutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPOutputStream

/**
 * Almost implementation copied from JsonMarshaller because of private methods
 */
class GlitchtipJsonMarshaller @JvmOverloads constructor(
    /**
     * Maximum length for a message.
     */
    private val maxMessageLength: Int = DEFAULT_MAX_MESSAGE_LENGTH
) : JsonMarshaller(maxMessageLength) {
    private val interfaceBindings: MutableMap<Class<out SentryInterface>, InterfaceBinding<*>> =
        HashMap<Class<out SentryInterface>, InterfaceBinding<*>>()

    @Throws(IOException::class)
    override fun marshall(event: Event, destination: OutputStream) {
        // Prevent the stream from being closed automatically
        var destination = destination
        destination = Marshaller.UncloseableOutputStream(destination)

        if (this.isCompressed) {
            destination = GZIPOutputStream(destination)
        }

        try {
            createJsonGenerator(destination).use { generator ->
                writeContent(generator, event)
            }
        } catch (e: IOException) {
            logger.error("An exception occurred while serialising the event.", e)
        } finally {
            try {
                destination.close()
            } catch (e: IOException) {
                logger.error("An exception occurred while serialising the event.", e)
            }
        }
    }

    @Throws(IOException::class)
    private fun writeContent(generator: JsonGenerator, event: Event) {
        generator.writeStartObject()

        generator.writeStringField(EVENT_ID, formatId(event.id))
        generator.writeStringField(MESSAGE, Util.trimString(event.message, maxMessageLength))
        generator.writeStringField(TIMESTAMP, ISO_FORMAT.get()!!.format(event.timestamp))
        generator.writeStringField(LEVEL, formatLevel(event.level))
        generator.writeStringField(LOGGER, event.logger)
        generator.writeStringField(PLATFORM, event.platform)
        generator.writeStringField(CULPRIT, event.culprit)
        generator.writeStringField(TRANSACTION, event.transaction)
        writeSdk(generator, event.sdk)
        writeTags(generator, event.tags)
        writeBreadcumbs(generator, event.breadcrumbs)
        writeContexts(generator, event.contexts)
        generator.writeStringField(SERVER_NAME, event.serverName)
        generator.writeStringField(RELEASE, event.release)
        generator.writeStringField(DIST, event.dist)
        generator.writeStringField(ENVIRONMENT, event.environment)
        writeExtras(generator, event.getExtra())
        writeCollection(generator, FINGERPRINT, event.fingerprint)
        generator.writeStringField(CHECKSUM, event.checksum)
        writeInterfaces(generator, event.sentryInterfaces)

        generator.writeEndObject()
    }

    @Throws(IOException::class)
    private fun writeInterfaces(generator: JsonGenerator, sentryInterfaces: MutableMap<String, SentryInterface>) {
        for (interfaceEntry in sentryInterfaces.entries) {
            val sentryInterface: SentryInterface = interfaceEntry.value

            if (interfaceBindings.containsKey(sentryInterface.javaClass)) {
                if (interfaceEntry.key == "sentry.interfaces.Exception") {
                    generator.writeFieldName("exception")
                    generator.writeStartObject()
                    generator.writeFieldName("values")
                    getInterfaceBinding<SentryInterface>(sentryInterface)!!.writeInterface(
                        generator,
                        interfaceEntry.value
                    )
                    generator.writeEndObject()
                } else {
                    generator.writeFieldName(interfaceEntry.key)
                    getInterfaceBinding<SentryInterface>(sentryInterface)!!.writeInterface(
                        generator,
                        interfaceEntry.value
                    )
                }
            } else {
                logger.error(
                    "Couldn't parse the content of '{}' provided in {}.",
                    interfaceEntry.key, sentryInterface
                )
            }
        }
    }

    private fun <T : SentryInterface> getInterfaceBinding(sentryInterface: T): InterfaceBinding<in T>? {
        // Reduces the @SuppressWarnings to a oneliner
        return interfaceBindings[sentryInterface.javaClass] as InterfaceBinding<in T>?
    }

    @Throws(IOException::class)
    private fun writeExtras(generator: JsonGenerator, extras: MutableMap<String, Any?>) {
        generator.writeObjectFieldStart(EXTRA)
        for (extra in extras.entries) {
            generator.writeFieldName(extra.key)
            generator.writeObject(extra.value)
        }
        generator.writeEndObject()
    }

    @Throws(IOException::class)
    private fun writeCollection(generator: JsonGenerator, name: String, value: MutableCollection<String>?) {
        if (value != null && !value.isEmpty()) {
            generator.writeArrayFieldStart(name)
            for (element in value) {
                generator.writeString(element)
            }
            generator.writeEndArray()
        }
    }

    @Throws(IOException::class)
    private fun writeSdk(generator: JsonGenerator, sdk: Sdk) {
        generator.writeObjectFieldStart(SDK)
        generator.writeStringField("name", sdk.name)
        generator.writeStringField("version", sdk.version)
        if (sdk.integrations != null && !sdk.integrations.isEmpty()) {
            generator.writeArrayFieldStart("integrations")
            for (integration in sdk.integrations) {
                generator.writeString(integration)
            }
            generator.writeEndArray()
        }
        generator.writeEndObject()
    }

    @Throws(IOException::class)
    private fun writeTags(generator: JsonGenerator, tags: MutableMap<String, String?>) {
        generator.writeObjectFieldStart(TAGS)
        for (tag in tags.entries) {
            generator.writeStringField(tag.key, tag.value)
        }
        generator.writeEndObject()
    }

    @Throws(IOException::class)
    private fun writeBreadcumbs(generator: JsonGenerator, breadcrumbs: MutableList<Breadcrumb>) {
        if (breadcrumbs.isEmpty()) {
            return
        }

        generator.writeObjectFieldStart(BREADCRUMBS)
        generator.writeArrayFieldStart("values")
        for (breadcrumb in breadcrumbs) {
            generator.writeStartObject()
            val tz = TimeZone.getTimeZone("UTC")
            val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            df.timeZone = tz
            generator.writeStringField("timestamp", df.format(breadcrumb.timestamp))

            if (breadcrumb.type != null) {
                generator.writeStringField("type", breadcrumb.type.value)
            }
            if (breadcrumb.level != null) {
                generator.writeStringField("level", breadcrumb.level.value)
            }
            if (breadcrumb.message != null) {
                generator.writeStringField("message", breadcrumb.message)
            }
            if (breadcrumb.category != null) {
                generator.writeStringField("category", breadcrumb.category)
            }
            if (breadcrumb.data != null && !breadcrumb.data.isEmpty()) {
                generator.writeObjectFieldStart("data")
                for (entry in breadcrumb.data.entries) {
                    generator.writeStringField(entry.key, entry.value)
                }
                generator.writeEndObject()
            }
            generator.writeEndObject()
        }
        generator.writeEndArray()
        generator.writeEndObject()
    }

    @Throws(IOException::class)
    private fun writeContexts(generator: JsonGenerator, contexts: MutableMap<String, MutableMap<String, Any?>>) {
        if (contexts.isEmpty()) {
            return
        }

        generator.writeObjectFieldStart(CONTEXTS)
        for (contextEntry in contexts.entries) {
            generator.writeObjectFieldStart(contextEntry.key)
            for (innerContextEntry in contextEntry.value.entries) {
                generator.writeObjectField(innerContextEntry.key, innerContextEntry.value)
            }
            generator.writeEndObject()
        }
        generator.writeEndObject()
    }

    /**
     * Formats the `UUID` to send only the 32 necessary characters.
     *
     * @param id uuid to format.
     * @return a `UUID` stripped from the "-" characters.
     */
    private fun formatId(id: UUID): String {
        return id.toString().replace("-".toRegex(), "")
    }

    /**
     * Formats a log level into one of the accepted string representation of a log level.
     *
     * @param level log level to format.
     * @return log level as a String.
     */
    private fun formatLevel(level: Event.Level?): String? {
        if (level == null) {
            return null
        }

        return when (level) {
            Event.Level.DEBUG -> "debug"
            Event.Level.FATAL -> "fatal"
            Event.Level.WARNING -> "warning"
            Event.Level.INFO -> "info"
            Event.Level.ERROR -> "error"
        }
    }

    /**
     * Add an interface binding to send a type of [SentryInterface] through a JSON stream.
     *
     * @param sentryInterfaceClass Actual type of SentryInterface supported by the [InterfaceBinding]
     * @param binding              InterfaceBinding converting SentryInterfaces of type `sentryInterfaceClass`.
     * @param <T> Type of SentryInterface received by the InterfaceBinding.
     * @param <F> Type of the interface stored in the event to send to the InterfaceBinding.
    </F></T> */
    override fun <T : SentryInterface, F : T> addInterfaceBinding(
        sentryInterfaceClass: Class<F>,
        binding: InterfaceBinding<T>
    ) {
        this.interfaceBindings.put(sentryInterfaceClass, binding)
    }

    companion object {
        /**
         * Date format for ISO 8601.
         */
        private val ISO_FORMAT: ThreadLocal<DateFormat> = object : ThreadLocal<DateFormat>() {
            override fun initialValue(): DateFormat {
                val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
                dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                return dateFormat
            }
        }

        private val logger: Logger = LoggerFactory.getLogger(JsonMarshaller::class.java)
    }
}
