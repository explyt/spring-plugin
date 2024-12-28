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

package com.explyt.spring.initializr

import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.intellij.icons.AllIcons
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.components.StorageScheme
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import org.cef.browser.CefBrowser
import org.cef.callback.CefBeforeDownloadCallback
import org.cef.callback.CefDownloadItem
import org.cef.callback.CefDownloadItemCallback
import org.cef.handler.CefDownloadHandler
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.io.File
import java.nio.file.Paths
import javax.swing.*
import javax.swing.border.LineBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener


class SpringInitializrWizardStep(private val context: WizardContext) : ModuleWizardStep() {
    private var serverUrl = getPropertyValue("explyt.spring.initializr.property.url")
    private val activeProject = ProjectUtil.getActiveProject()
    private var contentToolWindow: SimpleToolWindowPanel = SimpleToolWindowPanel(true, true)
    private val logger = Logger.getInstance(SpringInitializrWizardStep::class.java)

    var projectsDirectory = getActiveDirectory()

    var downloadFullPath: String? = null
    var zipFilePath: String? = null

    init {
        contentToolWindow.preferredSize = Dimension(550, 720)
        contentToolWindow.setBorder(JBUI.Borders.empty(20))
        contentToolWindow.layout = BorderLayout(10, 10)

        val info = getArchiveInfo()
        val progressBarWrapper = getProgress()

        val bottomPanel = BorderLayoutPanel(0, 10)
        bottomPanel.add(info, BorderLayout.NORTH)
        bottomPanel.add(progressBarWrapper, BorderLayout.SOUTH)

        val browser = getStartSpringBrowser(progressBarWrapper, info)
        contentToolWindow.add(getProjectLocation(browser), BorderLayout.NORTH)
        contentToolWindow.add(browser.component, BorderLayout.CENTER)
        contentToolWindow.add(bottomPanel, BorderLayout.SOUTH)
    }

    private fun getActiveDirectory(): File? {
        val path = getPropertyValue("explyt.spring.initializr.property.location")
        val propertyFile = if (path != null) LocalFileSystem.getInstance().findFileByPath(path) else null
        val vFile = propertyFile ?: activeProject?.guessProjectDir() ?: return null
        return File(vFile.path)
    }

    override fun getComponent(): JComponent {
        return contentToolWindow
    }

    override fun validate(): Boolean {
        if (!checkDownloadArchive()) {
            return false
        }
        return checkDirectoryForUnzip()
    }

    override fun updateDataModel() {
        zipFilePath?.let { context.projectName = FileUtil.getNameWithoutExtension(it) }
        context.projectStorageFormat = StorageScheme.DEFAULT
    }

    private fun checkDownloadArchive(): Boolean {
        val result = !downloadFullPath.isNullOrBlank() && !zipFilePath.isNullOrBlank()

        if (!result) {
            Messages.showWarningDialog(
                contentToolWindow,
                SpringInitializrBundle.message("explyt.spring.initializr.download.archive.message"),
                "Warning"
            )
        }
        return result
    }

    private fun checkDirectoryForUnzip(): Boolean {
        val projectPath = projectsDirectory?.path ?: return false
        val zipPath = zipFilePath ?: return false
        val unzip = Paths.get(projectPath, FileUtil.getNameWithoutExtension(zipPath))
        val file = File(unzip.toString())
        if (file.isDirectory) {
            Messages.showWarningDialog(
                contentToolWindow,
                "Directory '$unzip' is not empty!\nSelect a different location",
                "Warning"
            )
            return false
        }
        return true
    }

    private fun getProjectLocation(browser: JBCefBrowser): JPanel {
        val topPanel = BorderLayoutPanel(15, 5)

        val urlPanel = BorderLayoutPanel(0, 0)
        urlPanel.layout = BorderLayout(20, 0)
        val locationPanel = BorderLayoutPanel(0, 0)
        locationPanel.layout = BorderLayout(25, 0)

        if (serverUrl.isNullOrEmpty()) {
            serverUrl = SpringInitializrBundle.message("explyt.spring.initializr.url")
        }
        val label = JLabel("Server url:")
        urlPanel.add(label, BorderLayout.WEST)
        val labelUrl = JLabel(getUrl())
        urlPanel.add(labelUrl)
        val button = JButton(AllIcons.Actions.Edit)
        button.addActionListener {
            urlAction(labelUrl)
            val url = serverUrl
            if (!url.isNullOrBlank()) {
                browser.loadURL(url)
            }
        }
        urlPanel.add(button, BorderLayout.EAST)

        val labelLocation = JLabel("Location:")
        labelLocation.toolTipText =
            SpringInitializrBundle.message("explyt.spring.initializr.label.location.tool.tip.text")
        locationPanel.add(labelLocation, BorderLayout.WEST)

        val extendable = ExtendableTextField(10)
        val textFieldLocation = TextFieldWithBrowseButton(extendable)
        textFieldLocation.text = projectsDirectory?.path ?: ""
        addAction(textFieldLocation)
        locationPanel.add(textFieldLocation, BorderLayout.CENTER)

        topPanel.add(urlPanel, BorderLayout.NORTH)
        topPanel.add(locationPanel, BorderLayout.SOUTH)

        return topPanel
    }

    private fun addAction(textFieldLocation: TextFieldWithBrowseButton) {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        descriptor.title = SpringInitializrBundle.message("explyt.spring.initializr.label.descriptor.title")

        textFieldLocation.addBrowseFolderListener(object : TextBrowseFolderListener(descriptor, activeProject) {
            override fun onFileChosen(chosenFile: VirtualFile) {
                super.onFileChosen(chosenFile)
                val path = chosenFile.path
                if (path.isNotBlank()) {
                    projectsDirectory = File(chosenFile.path)
                    PropertiesComponent.getInstance()
                        .setValue("explyt.spring.initializr.property.location", chosenFile.path)
                    StatisticService.getInstance().addActionUsage(StatisticActionId.SPRING_INITIALIZR_FILE_CHOSEN)
                }
            }
        })

        val border = textFieldLocation.border
        textFieldLocation.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                warn()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                warn()
            }

            override fun changedUpdate(e: DocumentEvent?) {
                warn()
            }

            fun warn() {
                val path = textFieldLocation.text
                projectsDirectory = File(path)
                PropertiesComponent.getInstance()
                    .setValue("explyt.spring.initializr.property.location", path)

                val dir = projectsDirectory ?: return
                if (dir.isDirectory) {
                    textFieldLocation.border = border
                } else {
                    textFieldLocation.border = LineBorder(JBColor.YELLOW, 1)
                }
                StatisticService.getInstance().addActionUsage(StatisticActionId.SPRING_INITIALIZR_TEXT_LOCATION_CHANGE)
            }
        })
    }

    private fun urlAction(labelUrl: JLabel) {
        val result = Messages.showInputDialog(
            this.component,
            null,
            SpringInitializrBundle.message("explyt.spring.initializr.show.input.dialog.title"),
            null,
            serverUrl,
            null
        )
        if (!result.isNullOrBlank()) {
            serverUrl = result
            PropertiesComponent.getInstance().setValue("explyt.spring.initializr.property.url", serverUrl)
        }
        labelUrl.text = getUrl()
    }

    private fun getUrl() = "<html><a href=\"\">$serverUrl</a></html>"

    private fun getArchiveInfo(): JPanel {
        val infoPanel: JPanel = BorderLayoutPanel(15, 15)

        val label = JLabel("File name:")
        label.toolTipText = "Name of ZIP file"

        val fileNameZip = JTextField(" ")
        val buttonDelete = JButton("Delete file")
        fileNameZip.isEditable = false
        buttonDelete.isEnabled = false
        buttonDelete.addActionListener {
            fileNameZip.text = ""
            downloadFullPath = ""
            zipFilePath = ""
            buttonDelete.isEnabled = false
        }

        infoPanel.add(label, BorderLayout.WEST)
        infoPanel.add(fileNameZip, BorderLayout.CENTER)
        infoPanel.add(buttonDelete, BorderLayout.EAST)

        return infoPanel
    }

    private fun getProgress(): JPanel {
        val progressBarWrapper = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 0))
        progressBarWrapper.border = BorderFactory.createEmptyBorder(0, 0, 0, 0)

        val progressBarLabel = JLabel(" ")
        progressBarWrapper.add(progressBarLabel)

        val progressBar = JProgressBar()
        progressBar.isIndeterminate = false
        progressBarWrapper.add(progressBar)
        return progressBarWrapper
    }

    private fun getStartSpringBrowser(
        progress: JPanel,
        info: JPanel
    ): JBCefBrowser {
        val progressBarLabel = progress.components.first() as? JLabel
        val progressBar = progress.components.last() as? JProgressBar

        val eFileName = if (info.components.size > 1) info.components[1] as? JTextField else null
        val btnDelete = info.components.last() as? JButton

        val url = serverUrl ?: SpringInitializrBundle.message("explyt.spring.initializr.url")
        val browser = JBCefBrowser(url)
        if (activeProject == null) {
            return browser
        }

        browser.cefBrowser.createImmediately()

        val client = browser.jbCefClient
        client.addDownloadHandler(
            DownloadHandler(
                contentToolWindow,
                progressBar,
                progressBarLabel,
                eFileName,
                btnDelete
            ), browser.cefBrowser
        )

        return browser
    }

    inner class DownloadHandler(
        private val parent: JComponent,
        private val progressBar: JProgressBar?,
        private val progressBarLabel: JLabel?,
        private val fileName: JTextField?,
        private val bDelete: JButton?
    ) : CefDownloadHandler {

        private var downloadTimeoutTimer: Timer? = null

        override fun onBeforeDownload(
            browser: CefBrowser,
            downloadItem: CefDownloadItem,
            suggestedName: String,
            callback: CefBeforeDownloadCallback
        ) {
            logger.info("Download initiated. Suggested file name: $suggestedName")

            val zipFullPath = downloadFullPath
            zipFullPath?.let {
                val zipFile = File(it)
                if (zipFile.delete()) {
                    logger.info("Previous file deleted: $it")
                } else {
                    logger.warn("Failed to delete previous file: $it")
                }
            }
            parent.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
            progressBar?.isIndeterminate = true
            progressBarLabel?.text = SpringInitializrBundle.message(
                "explyt.spring.initializr.progress.label.text",
                suggestedName
            )
            logger.info("UI updated. Cursor set to wait, progress bar indeterminate.")

            callback.Continue(downloadItem.fullPath, false)
            logger.info("Callback continued with path: ${downloadItem.fullPath}")

            downloadTimeoutTimer = Timer(30_000) {
                logger.warn("Download timeout reached for file: $suggestedName")
                SwingUtilities.invokeLater {
                    parent.cursor = Cursor.getDefaultCursor()
                    progressBar?.isIndeterminate = false
                    progressBarLabel?.text = SpringInitializrBundle.message(
                        "explyt.spring.initializr.timeout.label.text",
                        suggestedName
                    )
                }
            }.apply { start() }
            logger.info("Download timeout timer started (30 seconds).")
        }

        override fun onDownloadUpdated(
            browser: CefBrowser,
            downloadItem: CefDownloadItem,
            callback: CefDownloadItemCallback
        ) {
            if (downloadItem.isInProgress) {
                val progress = (downloadItem.receivedBytes * 100 / downloadItem.totalBytes).toInt()
                progressBar?.value = progress
                logger.info("Download in progress. Received: ${downloadItem.receivedBytes}/${downloadItem.totalBytes} bytes. Progress: $progress%")
            } else if (downloadItem.isComplete) {
                logger.info("Download completed successfully. File path: ${downloadItem.fullPath}")
                try {
                    downloadFullPath = downloadItem.fullPath
                    zipFilePath = downloadItem.suggestedFileName
                    fileName?.text = zipFilePath
                    logger.info("File path updated in UI: $zipFilePath")
                } finally {
                    bDelete?.isEnabled = (!downloadFullPath.isNullOrBlank() && !zipFilePath.isNullOrBlank())
                    parent.cursor = Cursor.getDefaultCursor()
                    progressBar?.isIndeterminate = false
                    progressBarLabel?.text = " "
                    downloadTimeoutTimer?.stop()
                    logger.info("UI reset after successful download.")
                }
            } else if (downloadItem.isCanceled) {
                logger.warn("Download was cancelled.")
                handleCancelledDownload()
            }
        }

        private fun handleCancelledDownload() {
            SwingUtilities.invokeLater {
                parent.cursor = Cursor.getDefaultCursor()
                progressBar?.isIndeterminate = false
                progressBarLabel?.text = " "
                downloadTimeoutTimer?.stop()
            }
        }
    }

    private fun getPropertyValue(value: String): String? {
        return PropertiesComponent.getInstance().getValue(value)
    }

}
