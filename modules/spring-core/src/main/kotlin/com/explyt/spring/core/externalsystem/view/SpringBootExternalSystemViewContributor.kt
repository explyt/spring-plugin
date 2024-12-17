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

package com.explyt.spring.core.externalsystem.view

import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.explyt.spring.core.externalsystem.model.SpringBeanType
import com.explyt.spring.core.externalsystem.model.SpringProfileData
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.externalsystem.view.nodes.*
import com.explyt.spring.core.externalsystem.view.nodes.profile.SpringProfileNodes
import com.explyt.spring.core.externalsystem.view.nodes.profile.SpringProfileViewNode
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.view.ExternalProjectsView
import com.intellij.openapi.externalSystem.view.ExternalSystemNode
import com.intellij.openapi.externalSystem.view.ExternalSystemViewContributor
import com.intellij.util.containers.MultiMap

class SpringBootExternalSystemViewContributor : ExternalSystemViewContributor() {
    override fun getSystemId() = SYSTEM_ID

    override fun getKeys(): List<Key<*>> = listOf(SpringBeanData.KEY, SpringProfileData.KEY)

    override fun createNodes(
        externalProjectsView: ExternalProjectsView,
        dataNodes: MultiMap<Key<*>?, DataNode<*>?>
    ): List<ExternalSystemNode<*>> {
        val profileNodes = dataNodes[SpringProfileData.KEY]
        val profileViewNodes = profileNodes
            .map { SpringProfileViewNode(externalProjectsView, it as DataNode<SpringProfileData>) }

        val beanNodes = dataNodes[SpringBeanData.KEY]
        val projectBeans = mutableListOf<DataNode<SpringBeanData>>()
        val libraryBeans = mutableListOf<DataNode<SpringBeanData>>()
        beanNodes.mapNotNull { it as DataNode<SpringBeanData> }
            .forEach { if (it.data.projectBean) projectBeans.add(it) else libraryBeans.add(it) }
        val splitProjectBeans = splitBeansByType(projectBeans, externalProjectsView)
        val splitLibraryBeans = splitBeansByType(libraryBeans, externalProjectsView)
        return listOf(
            SpringProfileNodes(externalProjectsView, profileViewNodes),
            ProjectBeanNodes(externalProjectsView, toChildNodes(splitProjectBeans, externalProjectsView)),
            LibraryBeanNodes(externalProjectsView, toChildNodes(splitLibraryBeans, externalProjectsView)),
            *splitProjectBeans.applications.toTypedArray(),
        )
    }
}

private fun splitBeansByType(beanNodes: List<DataNode<SpringBeanData>>, view: ExternalProjectsView): SplitBeanHolder {
    val applications = mutableListOf<DataNode<SpringBeanData>>()
    val repositories = mutableListOf<DataNode<SpringBeanData>>()
    val controllers = mutableListOf<DataNode<SpringBeanData>>()
    val services = mutableListOf<DataNode<SpringBeanData>>()
    val components = mutableListOf<DataNode<SpringBeanData>>()
    val methods = mutableListOf<DataNode<SpringBeanData>>()
    val others = mutableListOf<DataNode<SpringBeanData>>()
    val configs = mutableListOf<DataNode<SpringBeanData>>()
    val configProperties = mutableListOf<DataNode<SpringBeanData>>()
    val autoConfigs = mutableListOf<DataNode<SpringBeanData>>()
    val messagesBroker = mutableListOf<DataNode<SpringBeanData>>()
    for (beanNode in beanNodes) {
        val beanData = beanNode.data
        val beanType = beanData.type
        when (beanType) {
            SpringBeanType.APPLICATION -> applications.add(beanNode)
            SpringBeanType.CONTROLLER -> controllers.add(beanNode)
            SpringBeanType.SERVICE -> services.add(beanNode)
            SpringBeanType.REPOSITORY -> repositories.add(beanNode)
            SpringBeanType.COMPONENT -> components.add(beanNode)
            SpringBeanType.METHOD -> methods.add(beanNode)
            SpringBeanType.CONFIGURATION -> configs.add(beanNode)
            SpringBeanType.CONFIGURATION_PROPERTIES -> configProperties.add(beanNode)
            SpringBeanType.AUTO_CONFIGURATION -> autoConfigs.add(beanNode)
            SpringBeanType.MESSAGE_MAPPING -> messagesBroker.add(beanNode)
            SpringBeanType.OTHER -> others.add(beanNode)
        }
    }
    return SplitBeanHolder(
        applications = toSpringBeanViewNodes(applications, view),
        repositories = toSpringBeanViewNodes(repositories, view),
        controllers = toSpringBeanViewNodes(controllers, view),
        services = toSpringBeanViewNodes(services, view),
        components = toSpringBeanViewNodes(components, view),
        methods = toSpringBeanViewNodes(methods, view),
        configs = toSpringBeanViewNodes(configs, view),
        configProperties = toSpringBeanViewNodes(configProperties, view),
        others = toSpringBeanViewNodes(others, view),
        autoConfigs = toSpringBeanViewNodes(autoConfigs, view),
        messagesBroker = toSpringBeanViewNodes(messagesBroker, view),
    )
}

private fun toChildNodes(
    splitBeanHolder: SplitBeanHolder, view: ExternalProjectsView
): List<ExternalSystemNode<*>> {
    val result = mutableListOf<ExternalSystemNode<*>>()
    if (splitBeanHolder.repositories.isNotEmpty()) {
        result.add(RepositoryBeanNodes(view, splitBeanHolder.repositories))
    }
    if (splitBeanHolder.controllers.isNotEmpty()) {
        result.add(ControllerBeanNodes(view, splitBeanHolder.controllers))
    }
    if (splitBeanHolder.services.isNotEmpty()) {
        result.add(ServiceBeanNodes(view, splitBeanHolder.services))
    }
    if (splitBeanHolder.components.isNotEmpty()) {
        result.add(ComponentBeanNodes(view, splitBeanHolder.components))
    }
    if (splitBeanHolder.methods.isNotEmpty()) {
        result.add(MethodBeanNodes(view, splitBeanHolder.methods))
    }
    if (splitBeanHolder.configs.isNotEmpty()) {
        result.add(ConfigBeanNodes(view, splitBeanHolder.configs))
    }
    if (splitBeanHolder.autoConfigs.isNotEmpty()) {
        result.add(AutoConfigBeanNodes(view, splitBeanHolder.autoConfigs))
    }
    if (splitBeanHolder.messagesBroker.isNotEmpty()) {
        result.add(MessageBrokerBeanNodes(view, splitBeanHolder.messagesBroker))
    }
    if (splitBeanHolder.others.isNotEmpty()) {
        result.add(OtherBeanNodes(view, splitBeanHolder.others))
    }
    if (splitBeanHolder.configProperties.isNotEmpty()) {
        result.add(ConfigPropertiesBeanNodes(view, splitBeanHolder.configProperties))
    }
    return result
}

private fun toSpringBeanViewNodes(
    buildActionNodes: Collection<DataNode<*>?>,
    externalProjectsView: ExternalProjectsView
) = buildActionNodes.map { SpringBeanViewNode(externalProjectsView, it as DataNode<SpringBeanData>) }

private data class SplitBeanHolder(
    val applications: List<SpringBeanViewNode>,
    val repositories: List<SpringBeanViewNode>,
    val controllers: List<SpringBeanViewNode>,
    val services: List<SpringBeanViewNode>,
    val components: List<SpringBeanViewNode>,
    val methods: List<SpringBeanViewNode>,
    val configs: List<SpringBeanViewNode>,
    val others: List<SpringBeanViewNode>,
    val configProperties: List<SpringBeanViewNode>,
    val autoConfigs: List<SpringBeanViewNode>,
    val messagesBroker: List<SpringBeanViewNode>,
)