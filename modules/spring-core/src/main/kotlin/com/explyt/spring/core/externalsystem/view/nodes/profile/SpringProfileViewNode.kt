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

package com.explyt.spring.core.externalsystem.view.nodes.profile

import com.explyt.spring.core.externalsystem.model.SpringProfileData
import com.explyt.spring.core.runconfiguration.SpringBootRunConfiguration
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
        val springRunConfiguration = RunManager.getInstance(project).allConfigurationsList
            .find { it is SpringBootRunConfiguration && it.name == profileData.configurationName }
                as? SpringBootRunConfiguration
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