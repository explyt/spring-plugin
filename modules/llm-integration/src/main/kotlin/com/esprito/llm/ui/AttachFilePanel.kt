package com.esprito.llm.ui

import com.esprito.llm.LlmBundle
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.nio.file.Path
import javax.swing.BoxLayout
import javax.swing.JPanel


class AttachFilePanel(private val project: Project) : JPanel() {

    @Volatile
    private var files = listOf<Path>()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        showFiles()
    }

    fun addFiles(newFiles: List<Path>) {
        val imageFile = newFiles.find { igmFilesExtension.any { ext -> it.toString().endsWith(ext) } }
        if (imageFile != null) {
            files = listOf(imageFile)
        } else {
            val noneImages = files.filter { igmFilesExtension.none { ext -> it.toString().endsWith(ext) } }
            files = (noneImages + newFiles).distinct()
        }
    }

    fun clearAll() {
        files = emptyList()
        showFiles()
    }

    fun showFiles() {
        removeAll()
        isVisible = files.isNotEmpty()
        if (files.isEmpty()) return
        if (files.size > 1) {
            val removeAllPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
            val removeAllButton = ActionLink(LlmBundle.message("esprito.gpt.history.remove.all"), ActionListener {
                files = emptyList()
                showFiles()
            })
            removeAllButton.setFont(JBUI.Fonts.smallFont())
            removeAllPanel.add(removeAllButton)
            add(removeAllPanel)
        }

        var rowAttachedPanel = JPanel()
        files.forEachIndexed { index, path ->
            if (index % 3 == 0) {
                rowAttachedPanel = JPanel(FlowLayout(FlowLayout.LEFT))
                add(rowAttachedPanel)
            }
            createFilePanel(path, rowAttachedPanel)
        }
    }

    fun getFiles(): AttachedFiles {
        val imageFile = files.find { igmFilesExtension.any { ext -> it.toString().endsWith(ext) } }
        if (imageFile != null) return AttachedFiles(emptyList(), imageFile)
        return AttachedFiles(files, null)
    }

    private fun createFilePanel(path: Path, jPanel: JPanel) {
        val virtualFile = LocalFileSystem.getInstance().findFileByNioFile(path) ?: return
        val actionLink = ActionLink(path.fileName.toString()) { _: ActionEvent? ->
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        }
        actionLink.icon = FileTypeManager.getInstance().getFileTypeByFile(virtualFile).icon
        actionLink.setFont(JBUI.Fonts.smallFont())


        val removeButton = ActionLink("x", ActionListener {
            files = files.filter { it != path }
            showFiles()
        })
        removeButton.setFont(JBUI.Fonts.smallFont())

        jPanel.add(actionLink)
        jPanel.add(removeButton)
    }

    companion object {
        val igmFilesExtension = listOf("jpg", "jpeg", "png")
    }
}

data class AttachedFiles(val textFiles: List<Path>, val imageFile: Path?) {
    fun allFiles(): List<Path> {
        if (imageFile != null) return listOf(imageFile)
        return textFiles
    }
}