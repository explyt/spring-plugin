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

package com.explyt.spring.web.view.nodes

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.web.SpringWebIcons
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.view.EndpointElementViewData
import com.intellij.icons.AllIcons
import com.intellij.pom.Navigatable
import com.intellij.ui.treeStructure.CachingSimpleNode
import com.intellij.ui.treeStructure.SimpleNode

class HttpMethodNode(
    val httpElement: EndpointElementViewData, rootNode: CachingSimpleNode
) : CachingSimpleNode(rootNode), EndpointNavigable {

    init {
        when (httpElement.method.uppercase()) {
            "CONNECT" -> presentation.setIcon(SpringWebIcons.HttpConnect)
            "DELETE" -> presentation.setIcon(SpringWebIcons.HttpDelete)
            "GET" -> presentation.setIcon(SpringWebIcons.HttpGet)
            "HEAD" -> presentation.setIcon(SpringWebIcons.HttpHead)
            "OPTIONS" -> presentation.setIcon(SpringWebIcons.HttpOptions)
            "PATCH" -> presentation.setIcon(SpringWebIcons.HttpPatch)
            "PUT" -> presentation.setIcon(SpringWebIcons.HttpPut)
            "POST" -> presentation.setIcon(SpringWebIcons.HttpPost)
            "TRACE" -> presentation.setIcon(SpringWebIcons.HttpTrace)

            EndpointType.MESSAGE_BROKER.name -> presentation.setIcon(AllIcons.Webreferences.MessageQueue)
            EndpointType.EVENT_LISTENERS.name -> presentation.setIcon(SpringIcons.EventListener)

            else -> presentation.setIcon(AllIcons.General.Web)
        }
    }

    override fun getName() = httpElement.path

    override fun buildChildren() = emptyArray<SimpleNode>()

    override fun navigate() {
        (httpElement.psiPointer.element?.navigationElement as? Navigatable)?.navigate(true)
    }
}