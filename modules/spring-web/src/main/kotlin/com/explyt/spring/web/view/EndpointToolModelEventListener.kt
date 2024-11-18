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

package com.explyt.spring.web.view

import com.intellij.openapi.Disposable
import com.intellij.util.messages.Topic

interface EndpointToolModelEventListener {

    fun handle(event: ModelEvent)

    data class ModelEvent(
        val type: EventType,
        val disposable: Disposable
    )

    enum class EventType {
        INIT, UPDATE_DATA, UPDATE_FILTERS
    }

    companion object {
        val TOPIC = Topic.create(
            "endpoint tool model topic",
            EndpointToolModelEventListener::class.java,
            Topic.BroadcastDirection.NONE
        )
    }

}