package com.esprito.spring.core.externalsystem.view.nodes

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.externalsystem.model.SpringBeanData
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.externalSystem.view.ExternalProjectsView
import com.intellij.openapi.externalSystem.view.ExternalSystemNode

@Order(5)
class SpringBeanViewNode(externalProjectsView: ExternalProjectsView, val dataNode: DataNode<SpringBeanData>) :
    ExternalSystemNode<SpringBeanData>(externalProjectsView, null, dataNode) {

    override fun update(presentation: PresentationData) {
        super.update(presentation)
        presentation.setIcon(SpringIcons.SpringBean)
    }

    override fun getName() = getBeanViewName()

    override fun getActionId() = "NavigateToSpringBean"

    private fun getBeanViewName(): String {
        val beanName = dataNode.data.beanName
        if (beanName.contains(".")) return beanName.substringAfterLast(".")
        return beanName
    }

    override fun isAlwaysLeaf() = true
}