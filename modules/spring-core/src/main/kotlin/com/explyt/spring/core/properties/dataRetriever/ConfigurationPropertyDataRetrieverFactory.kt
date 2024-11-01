package com.explyt.spring.core.properties.dataRetriever

import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter

class ConfigurationPropertyDataRetrieverFactory {

    companion object {
        fun createFor(uElement: UElement?): ConfigurationPropertyDataRetriever? {
            return when (uElement) {
                is UMethod -> MethodConfigurationPropertyDataRetriever.create(uElement)
                is UField -> FieldConfigurationPropertyDataRetriever.create(uElement)
                is UParameter -> ConstructorParameterConfigurationPropertyDataRetriever.create(uElement)
                else -> null
            }
        }
    }

}