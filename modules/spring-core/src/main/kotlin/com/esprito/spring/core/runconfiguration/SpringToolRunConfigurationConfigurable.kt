package com.esprito.spring.core.runconfiguration

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.action.UastModelTrackerInvalidateAction
import com.esprito.spring.core.runconfiguration.clients.EspritoLicenseClient
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.JBColor
import com.intellij.ui.SeparatorWithText
import com.intellij.ui.util.maximumHeight
import com.intellij.ui.util.minimumHeight
import com.intellij.uiDesigner.core.Spacer
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*


class SpringToolRunConfigurationConfigurable : Configurable, SearchableConfigurable {
    private val settingsState = SpringToolRunConfigurationsSettingsState.getInstance()

    private val propertyGraph = PropertyGraph()

    private val isAutoDetection = propertyGraph.lazyProperty { settingsState.isAutoDetectConfigurations }
    private val isBeanFilterEnabled = propertyGraph.lazyProperty { settingsState.isBeanFilterEnabled }
    private val sateLicense = propertyGraph.lazyProperty { settingsState.stateLicenseConfigurations }
    private val textLicense = propertyGraph.lazyProperty { settingsState.textLicenseConfigurations }

    private var labelOrganizationName =
        JLabel(propertyGraph.lazyProperty { settingsState.licenseOrganizationName }.get())
    private var labelEmail = JLabel(propertyGraph.lazyProperty { settingsState.licenseFullName }.get())
    private var labelFullName = JLabel(propertyGraph.lazyProperty { settingsState.licenseEmail }.get())
    private var labelEndDate = JLabel(propertyGraph.lazyProperty { settingsState.licenseEndDate }.get())

    override fun getId(): String = ID

    override fun createComponent(): JComponent {
        return createMainPanel()
    }

    override fun isModified(): Boolean {
        val changeDetection = isAutoDetection.get() != settingsState.isAutoDetectConfigurations
                || isBeanFilterEnabled.get() != settingsState.isBeanFilterEnabled
        val changeLicense = textLicense.get() != settingsState.textLicenseConfigurations

        return changeDetection
                || (changeLicense && sateLicense.get() == EspritoLicenseState.Valid.state)
    }

    override fun apply() {
        settingsState.isAutoDetectConfigurations = isAutoDetection.get()
        settingsState.textLicenseConfigurations = textLicense.get()
        settingsState.stateLicenseConfigurations = sateLicense.get()

        if (settingsState.isBeanFilterEnabled != isBeanFilterEnabled.get()) {
            settingsState.isBeanFilterEnabled = isBeanFilterEnabled.get()

            ProjectUtil.getActiveProject()?.let { project ->
                UastModelTrackerInvalidateAction.invalidate(project)
            }
        }


        if (sateLicense.get() == EspritoLicenseState.Valid.state) {
            settingsState.licenseOrganizationName = labelOrganizationName.text
            settingsState.licenseFullName = labelFullName.text
            settingsState.licenseEmail = labelEmail.text
            settingsState.licenseEndDate = labelEndDate.text
        }
    }

    override fun getDisplayName(): String = "Run Configurations"

    companion object {
        const val ID = "com.esprito.spring.runConfigurations"
    }

    private fun createMainPanel(): JPanel {
        val mainPanel = BorderLayoutPanel(10, 10)
        mainPanel.preferredSize = Dimension(550, 400)

        mainPanel.addToTop(configTop())
        mainPanel.addToCenter(configLicense())
        mainPanel.addToBottom(configBottom())

        return mainPanel
    }

    private fun configTop(): BorderLayoutPanel {
        val topPanel = BorderLayoutPanel(10, 10)

        val cbAutoDetection = JCheckBox("SpringBoot configurations auto-detection")
        cbAutoDetection.isSelected = isAutoDetection.get()
        cbAutoDetection.addItemListener {
            isAutoDetection.set((it.source as AbstractButton).isSelected)
        }
        topPanel.addToCenter(cbAutoDetection)

        val cbEnableBeanFiltering =
            JCheckBox(SpringCoreBundle.message("esprito.spring.settings.enableBeanFiltering.label"))
        cbEnableBeanFiltering.toolTipText =
            SpringCoreBundle.message("esprito.spring.settings.enableBeanFiltering.tooltip")
        cbEnableBeanFiltering.isSelected = isBeanFilterEnabled.get()
        cbEnableBeanFiltering.addItemListener {
            isBeanFilterEnabled.set((it.source as AbstractButton).isSelected)
        }
        topPanel.addToBottom(cbEnableBeanFiltering)

        return topPanel
    }

    private fun configLicense(): BorderLayoutPanel {
        val centerPanel = BorderLayoutPanel(10, 10)
        centerPanel.maximumHeight = 300

        val topPanel = BorderLayoutPanel(0, 0)
        val separator = SeparatorWithText()
        separator.caption = "License Info"

        val label = JLabel("Enter Your License:")
        topPanel.addToTop(separator)
        topPanel.addToBottom(label)

        val textArea = JTextArea()
        textArea.wrapStyleWord = true
        textArea.lineWrap = true
        textArea.autoscrolls = true
        textArea.minimumHeight = 200
        textArea.append(textLicense.get())

        val bottomCenterPanel = BorderLayoutPanel(5, 10)
        val buttonPanel = BorderLayoutPanel(5, 5)
        val validateButton = JButton("Validate")
        val licenseLabel = JLabel(getLabelLicenseText())
        val defaultColor = licenseLabel.foreground
        licenseLabel.foreground = getLabelLicenseColor(defaultColor)
        val panelLicenseInfo = getLicenseInfo()
        panelLicenseInfo.isVisible = sateLicense.get() == EspritoLicenseState.Valid.state

        validateButton.addActionListener {
            panelLicenseInfo.isVisible = false
            bottomCenterPanel.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
            val text = textArea.text
            try {
                if (text.isEmpty()) {
                    sateLicense.set(EspritoLicenseState.Empty.state)
                } else {
                    val result = verify(text)
                    sateLicense.set(result.state)
                    if (result == EspritoLicenseState.Valid) {
                        panelLicenseInfo.isVisible = true
                    }
                }
            } catch (e: Exception) {
                sateLicense.set(EspritoLicenseState.NotConnect.state)
            } finally {
                licenseLabel.text = getLabelLicenseText()
                licenseLabel.foreground = getLabelLicenseColor(defaultColor)
                bottomCenterPanel.cursor = Cursor.getDefaultCursor()
            }
            textLicense.set(textArea.text)
        }

        buttonPanel.addToLeft(validateButton)
        buttonPanel.addToRight(licenseLabel)
        bottomCenterPanel.addToTop(buttonPanel)
        bottomCenterPanel.addToBottom(panelLicenseInfo)

        centerPanel.addToTop(topPanel)
        centerPanel.addToCenter(textArea)
        centerPanel.addToBottom(bottomCenterPanel)

        enableLicensePanel(centerPanel, true)

        return centerPanel
    }

    private fun verify(pem: String): EspritoLicenseState {
        val request = EspritoLicenseClient.requestSign(pem)
        writeVerifyInfo(request)
        val verify = (request["verify"] as? Double)?.toInt() ?: return EspritoLicenseState.Unknown
        return EspritoLicenseState.entries.find { it.state == verify } ?: EspritoLicenseState.Unknown
    }

    private fun getLicenseInfo(): JPanel {
        val licenseInfo = JPanel()
        licenseInfo.setLayout(GridLayout(4, 4, 5, 5))
        licenseInfo.isVisible = false

        licenseInfo.add(JLabel("Organization name:"))
        licenseInfo.add(labelOrganizationName)
        licenseInfo.add(Spacer())
        licenseInfo.add(Spacer())
        licenseInfo.add(JLabel("Email:"))
        licenseInfo.add(labelEmail)
        licenseInfo.add(Spacer())
        licenseInfo.add(Spacer())
        licenseInfo.add(JLabel("Full name:"))
        licenseInfo.add(labelFullName)
        licenseInfo.add(Spacer())
        licenseInfo.add(Spacer())
        licenseInfo.add(JLabel("End date:"))
        licenseInfo.add(labelEndDate)
        licenseInfo.add(Spacer())
        licenseInfo.add(Spacer())
        return licenseInfo
    }

    private fun configBottom(): BorderLayoutPanel {
        val bottomPanel = BorderLayoutPanel(0, 300)
        bottomPanel.minimumHeight = 300
        return bottomPanel
    }

    private fun getLabelLicenseText(): String {
        return when (sateLicense.get()) {
            EspritoLicenseState.NotValid.state -> "License is not valid"
            EspritoLicenseState.Valid.state -> "License is valid"
            EspritoLicenseState.Expired.state -> "License is expired"
            EspritoLicenseState.Empty.state -> "License is empty"
            EspritoLicenseState.NotConnect.state -> "No connection to server"
            else -> ""
        }
    }

    private fun getLabelLicenseColor(defaultColor: Color): Color {
        return when (sateLicense.get()) {
            EspritoLicenseState.NotValid.state -> JBColor.RED
            EspritoLicenseState.Expired.state -> JBColor.YELLOW
            EspritoLicenseState.Valid.state -> defaultColor
            else -> JBColor.GRAY
        }
    }

    private fun enableLicensePanel(centerPanel: BorderLayoutPanel, enable: Boolean) {
        centerPanel.isEnabled = enable
        for (it in centerPanel.components) {
            it.isEnabled = enable
        }
    }

    private fun writeVerifyInfo(info: Map<String, Any?>) {
        labelOrganizationName.text = (info["organizationName"] ?: "").toString()
        labelEmail.text = (info["email"] ?: "").toString()
        labelFullName.text = (info["fullName"] ?: "").toString()
        labelEndDate.text = (info["endDate"] ?: "").toString()
    }
}
