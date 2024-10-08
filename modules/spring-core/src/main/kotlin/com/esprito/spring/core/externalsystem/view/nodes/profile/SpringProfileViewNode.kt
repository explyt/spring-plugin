package com.esprito.spring.core.externalsystem.view.nodes.profile

import com.esprito.spring.core.externalsystem.model.SpringProfileData
import com.esprito.spring.core.runconfiguration.SpringBootRunConfiguration
import com.intellij.execution.RunManager
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.view.ExternalProjectsView
import com.intellij.openapi.externalSystem.view.ExternalSystemNode


class SpringProfileViewNode(
    externalProjectsView: ExternalProjectsView, private val dataNode: DataNode<SpringProfileData>
) :
    ExternalSystemNode<SpringProfileData>(externalProjectsView, null, dataNode) {

    override fun update(presentation: PresentationData) {
        super.update(presentation)
        presentation.setIcon(AllIcons.Diff.GutterCheckBox)
        val profileData = data ?: return
        val springRunConfiguration = RunManager.getInstance(project)
            .allConfigurationsList.find { it.name == profileData.configurationName } as? SpringBootRunConfiguration
        if (springRunConfiguration == null) {
            presentation.setIcon(AllIcons.Diff.GutterCheckBox)
        } else {
            val profiles = springRunConfiguration.springProfiles?.replace(" ", "") ?: ""
            if (isSelected(profiles, profileData)) {
                presentation.setIcon(AllIcons.Diff.GutterCheckBoxSelected)
            } else {
                presentation.setIcon(AllIcons.Diff.GutterCheckBox)
            }
        }
    }

    private fun isSelected(profiles: String, profileData: SpringProfileData) =
        profiles.contains("${profileData.name},") || profiles.endsWith(profileData.name)

    override fun getName() = dataNode.data.name

    override fun getActionId() = "Explyt.ExternalView.ProfileChanged"

    override fun isAlwaysLeaf() = true
}