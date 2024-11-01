package com.explyt.spring.core.externalsystem.view.nodes.profile

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.externalSystem.view.ExternalProjectsView
import com.intellij.openapi.externalSystem.view.ExternalSystemNode

@Order(7)
class SpringProfileNodes(externalProjectsView: ExternalProjectsView, val profiles: List<SpringProfileViewNode>) :
    ExternalSystemNode<SpringBeanData>(externalProjectsView, null, null) {

    override fun update(presentation: PresentationData) {
        super.update(presentation)
        presentation.setIcon(AllIcons.Nodes.ConfigFolder)
        if (profiles.isNotEmpty()) {
            val configurationName = profiles.first().data!!.configurationName
            presentation.tooltip = "Profiles from SpringBootRunConfiguration - $configurationName"
        }
    }

    override fun getName(): String {
        return SpringCoreBundle.message("explyt.external.view.node.profile")
    }

    override fun isVisible() = super.isVisible() && hasChildren()

    override fun doBuildChildren() = profiles

}