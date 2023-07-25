package com.esprito.spring.core.runconfiguration

import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class SpringToolRunConfigurationConfigurable : Configurable, SearchableConfigurable {
    private val settingsState = SpringToolRunConfigurationsSettingsState.getInstance()

    private val propertyGraph = PropertyGraph()

    private val isAutoDetection = propertyGraph.lazyProperty { settingsState.isAutoDetectConfigurations }

    override fun getId(): String = ID

    override fun createComponent(): JComponent {
        return panel {
            row {
                checkBox("SpringBoot configurations auto-detection")
                    .bindSelected(isAutoDetection)
            }
        }
    }

    override fun isModified(): Boolean {
        return settingsState.isAutoDetectConfigurations != isAutoDetection.get()
    }

    override fun apply() {
        settingsState.isAutoDetectConfigurations = isAutoDetection.get()
    }

    override fun getDisplayName(): String = "Run Configurations"

    companion object {
        const val ID = "com.esprito.spring.runConfigurations"
    }
}