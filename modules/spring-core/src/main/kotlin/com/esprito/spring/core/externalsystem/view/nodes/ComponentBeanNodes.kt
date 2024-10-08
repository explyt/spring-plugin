package com.esprito.spring.core.externalsystem.view.nodes

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.externalsystem.model.SpringBeanData
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.externalSystem.view.ExternalProjectsView
import com.intellij.openapi.externalSystem.view.ExternalSystemNode

@Order(40)
class ComponentBeanNodes(externalProjectsView: ExternalProjectsView, val beans: List<SpringBeanViewNode>) :
    ExternalSystemNode<SpringBeanData>(externalProjectsView, null, null) {

    override fun update(presentation: PresentationData) {
        super.update(presentation)
        presentation.setIcon(AllIcons.Nodes.ConfigFolder)
    }

    override fun getName() = SpringCoreBundle.message("explyt.external.view.node.component")

    override fun isVisible() = super.isVisible() && hasChildren()

    override fun doBuildChildren() = beans

}