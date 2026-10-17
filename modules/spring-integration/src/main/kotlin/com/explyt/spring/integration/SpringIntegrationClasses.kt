/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.integration

object SpringIntegrationClasses {
    const val NULL_CHANNEL = "org.springframework.integration.channel.NullChannel"
    const val DIRECT_CHANNEL = "org.springframework.integration.channel.DirectChannel"
    const val INTEGRATION_FLOW_CONTEXT = "org.springframework.integration.dsl.context.IntegrationFlowContext"
    const val MESSAGING_GATEWAY = "org.springframework.integration.annotation.MessagingGateway"
    const val INTEGRATION_COMPONENT_SCAN = "org.springframework.integration.annotation.IntegrationComponentScan"
    const val ENABLE_AUTO_CONFIGURATION = "org.springframework.boot.autoconfigure.EnableAutoConfiguration"
    const val SPRING_BOOT_APPLICATION = "org.springframework.boot.autoconfigure.SpringBootApplication"
    val INTEGRATION_AUTO_CONFIGURATIONS = listOf(
        "org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration",
        "org.springframework.boot.integration.autoconfigure.IntegrationAutoConfiguration"
    )
}