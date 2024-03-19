package com.esprito.spring.core.runconfiguration

import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.SeparatorWithText
import com.intellij.ui.util.maximumHeight
import com.intellij.ui.util.minimumHeight
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Dimension
import javax.swing.*


class SpringToolRunConfigurationConfigurable : Configurable, SearchableConfigurable {
    private val settingsState = SpringToolRunConfigurationsSettingsState.getInstance()

    private val propertyGraph = PropertyGraph()

    private val isAutoDetection = propertyGraph.lazyProperty { settingsState.isAutoDetectConfigurations }
    private val sateLicense = propertyGraph.lazyProperty { settingsState.stateLicenseConfigurations }
    private val textLicense = propertyGraph.lazyProperty { settingsState.textLicenseConfigurations }

    override fun getId(): String = ID

    override fun createComponent(): JComponent {
        return createMainPanel()
    }

    override fun isModified(): Boolean {
        val changeDetection = isAutoDetection.get() != settingsState.isAutoDetectConfigurations
        val changeLicense = sateLicense.get() != settingsState.stateLicenseConfigurations

        return changeDetection || changeLicense
    }

    override fun apply() {
        settingsState.isAutoDetectConfigurations = isAutoDetection.get()
        settingsState.textLicenseConfigurations = textLicense.get()
    }

    override fun getDisplayName(): String = "Run Configurations"

    companion object {
        const val ID = "com.esprito.spring.runConfigurations"
    }

    private fun createMainPanel(): JPanel {
        val mainPanel = BorderLayoutPanel(10, 10)
        mainPanel.preferredSize = Dimension(550, 400)

        val bottomPanel = BorderLayoutPanel(0, 300)
        bottomPanel.minimumHeight = 300

        val topPanel = configAutoDetection()
        val centerPanel = configLicense()

        mainPanel.addToTop(topPanel)
        mainPanel.addToCenter(centerPanel)
        mainPanel.addToBottom(bottomPanel)

        return mainPanel
    }

    private fun configAutoDetection(): BorderLayoutPanel {
        val topPanel = BorderLayoutPanel(10, 10)

        val cbAutoDetection = JCheckBox("SpringBoot configurations auto-detection")
        cbAutoDetection.isSelected = isAutoDetection.get()
        cbAutoDetection.addItemListener {
            isAutoDetection.set((it.source as AbstractButton).isSelected)
        }
        topPanel.addToCenter(cbAutoDetection)

        return topPanel
    }

    private fun configLicense(): BorderLayoutPanel {
        val centerPanel = BorderLayoutPanel(10, 10)
        centerPanel.maximumHeight = 100

        val topPanel = BorderLayoutPanel(0, 0)
        val separator = SeparatorWithText()
        separator.caption = "License Info"

        val label = JLabel("Enter Your License:")
        topPanel.addToTop(separator)
        topPanel.addToBottom(label)

        val textArea = JTextArea()
        textArea.append(textLicense.get())

        val buttonPanel = BorderLayoutPanel(5, 5)
        val validateButton = JButton("Validate")
        val licenseLabel = JLabel(getLabelValidText())

        validateButton.addActionListener {
            if (textArea.text.isEmpty()) {
                sateLicense.set(LicenseState.Empty.state)
            } else {
                sateLicense.set(LicenseState.NotValid.state)
            }
            licenseLabel.text = getLabelValidText()
            textLicense.set(textArea.text)
        }

        buttonPanel.addToLeft(validateButton)
        buttonPanel.addToRight(licenseLabel)

        centerPanel.addToTop(topPanel)
        centerPanel.addToCenter(textArea)
        centerPanel.addToBottom(buttonPanel)

        textArea.isEnabled = false
        validateButton.isEnabled = false
        centerPanel.isEnabled = false

        return centerPanel
    }

    private fun getLabelValidText(): String {
        return when (sateLicense.get()) {
            LicenseState.NotValid.state -> "License not valid"
            LicenseState.Valid.state -> "License is valid"
            LicenseState.Empty.state -> "License is empty"
            LicenseState.Unknown.state -> ""
            else -> ""
        }
    }

}

enum class LicenseState(val state: Int) {
    Unknown(-1),
    NotValid(0),
    Valid(1),
    Empty(2)
}