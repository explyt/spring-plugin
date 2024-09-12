package com.esprito.llm.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import javax.swing.*

class SelectedFilesAccordion(
    project: Project, referencedFilePaths: List<String>
) : JPanel(BorderLayout()) {
    init {
        isOpaque = false

        val contentPanel = createContentPanel(project, referencedFilePaths)
        add(createToggleButton(contentPanel, referencedFilePaths.size), BorderLayout.NORTH)
        add(contentPanel, BorderLayout.CENTER)
    }

    private fun createContentPanel(project: Project, referencedFilePaths: List<String>): JPanel {
        val panel = JPanel()
        panel.isOpaque = false
        panel.isVisible = false
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = JBUI.Borders.empty(4, 0)
        referencedFilePaths.asSequence()
            .mapNotNull { LocalFileSystem.getInstance().findFileByPath(it) }
            .map {
                val actionLink = ActionLink(it.name) { _: ActionEvent? ->
                    FileEditorManager.getInstance(project).openFile(it, true)
                }
                actionLink.icon = FileTypeManager.getInstance().getFileTypeByFile(it).icon
                actionLink.setFont(JBUI.Fonts.smallFont())
                actionLink
            }.forEach {
                panel.add(it)
                panel.add(Box.createVerticalStrut(4))
            }
        return panel
    }

    private fun createToggleButton(contentPane: JPanel, fileCount: Int): JToggleButton {
        val accordionToggle = JToggleButton(
            String.format("Referenced files (+%d)", fileCount), AllIcons.General.ArrowDown
        )
        accordionToggle.isFocusPainted = false
        accordionToggle.isContentAreaFilled = false
        accordionToggle.background = background
        accordionToggle.selectedIcon = AllIcons.General.ArrowUp
        accordionToggle.border = null
        accordionToggle.horizontalAlignment = SwingConstants.LEADING
        accordionToggle.horizontalTextPosition = SwingConstants.LEADING
        accordionToggle.addItemListener { e: ItemEvent -> contentPane.isVisible = e.stateChange == ItemEvent.SELECTED }
        return accordionToggle
    }
}
