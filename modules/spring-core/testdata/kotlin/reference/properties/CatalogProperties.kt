/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package src

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "explyt.catalog")
data class CatalogProperties(
    val providers: List<Provider> = emptyList(),
    val regions: Map<String, Provider> = emptyMap(),
) {
    data class Provider(
        val url: String = "",
        val models: Map<String, Model> = emptyMap(),
    )

    data class Model(
        val availableForPersonal: Boolean = false,
        val modelInfo: ModelInfo = ModelInfo(),
    )

    data class ModelInfo(
        val modelName: String = "",
    )
}
