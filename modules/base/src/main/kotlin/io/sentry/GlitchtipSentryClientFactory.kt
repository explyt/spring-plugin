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

package io.sentry

import io.sentry.config.Lookup
import io.sentry.dsn.Dsn
import io.sentry.event.interfaces.*
import io.sentry.marshaller.Marshaller
import io.sentry.marshaller.json.*

/**
 * Default implementation of [SentryClientFactory].
 *
 *
 * In most cases this is the implementation to use or extend for additional features.
 */
class GlitchtipSentryClientFactory @JvmOverloads constructor(lookup: Lookup = Lookup.getDefault()) : DefaultSentryClientFactory(lookup) {

    /**
     * Creates a JSON marshaller that will convert every [io.sentry.event.Event] in a format
     * handled by the Sentry server.
     *
     * @param dsn Data Source Name of the Sentry server.
     * @return a [JsonMarshaller] to process the events.
     */
    override fun createMarshaller(dsn: Dsn?): Marshaller {
        val maxMessageLength = getMaxMessageLength(dsn)
        val marshaller = createJsonMarshaller(maxMessageLength)

        // Set JSON marshaller bindings
        val stackTraceBinding = StackTraceInterfaceBinding()
        // Enable common frames hiding unless its value is 'false'.
        stackTraceBinding.setRemoveCommonFramesWithEnclosing(getHideCommonFramesEnabled(dsn))
        stackTraceBinding.setInAppFrames(getInAppFrames(dsn))

        marshaller.addInterfaceBinding(
            StackTraceInterface::class.java,
            stackTraceBinding
        )
        marshaller.addInterfaceBinding(
            ExceptionInterface::class.java,
            ExceptionInterfaceBinding(stackTraceBinding)
        )
        marshaller.addInterfaceBinding(
            MessageInterface::class.java,
            MessageInterfaceBinding(maxMessageLength)
        )
        marshaller.addInterfaceBinding(
            UserInterface::class.java,
            UserInterfaceBinding()
        )
        marshaller.addInterfaceBinding(
            DebugMetaInterface::class.java,
            DebugMetaInterfaceBinding()
        )
        val httpBinding = HttpInterfaceBinding()
        //TODO: Add a way to clean the HttpRequest
        //httpBinding.
        marshaller.addInterfaceBinding(HttpInterface::class.java, httpBinding)

        // Enable compression unless the option is set to false
        marshaller.setCompression(getCompressionEnabled(dsn))

        return marshaller
    }

    override fun createJsonMarshaller(maxMessageLength: Int): JsonMarshaller {
        return GlitchtipJsonMarshaller(maxMessageLength)
    }
}