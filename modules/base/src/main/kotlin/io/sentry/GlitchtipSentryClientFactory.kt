/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package io.sentry

import io.sentry.config.Lookup
import io.sentry.marshaller.json.GlitchtipJsonMarshaller
import io.sentry.marshaller.json.JsonMarshaller

class GlitchtipSentryClientFactory @JvmOverloads constructor(lookup: Lookup = Lookup.getDefault()) : DefaultSentryClientFactory(lookup) {

    override fun createJsonMarshaller(maxMessageLength: Int): JsonMarshaller {
        return GlitchtipJsonMarshaller(maxMessageLength)
    }
}