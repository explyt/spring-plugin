package com.esprito.spring.web.view

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