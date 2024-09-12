package com.esprito.llm.util

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension

private val DEFAULT_PROMPT_TEMPLATE = """
          Use the following context to answer question at the end:

          {REPEATABLE_CONTEXT}

          Question: {QUESTION}

          """.trimIndent()

private val DEFAULT_REPEATABLE_CONTEXT = """
          File Path: {FILE_PATH}
          File Content:
          {FILE_CONTENT}
          """.trimIndent()

object GptUtil {

    @JvmStatic
    fun splitMdCodeBlocks(inputMarkdown: String?): List<String> {
        inputMarkdown ?: return emptyList()
        val result: MutableList<String> = ArrayList()
        val pattern = Pattern.compile("(?s)```.*?```")
        val matcher = pattern.matcher(inputMarkdown)
        var start = 0
        while (matcher.find()) {
            result.add(inputMarkdown.substring(start, matcher.start()))
            result.add(matcher.group())
            start = matcher.end()
        }
        result.add(inputMarkdown.substring(start))
        return result.stream().filter(String::isNotBlank).toList()
    }

    fun md2html(markdown: String): String {
        val parser = Parser.builder().build()
        val document: Node = parser.parse(markdown)
        val htmlRenderer = HtmlRenderer.builder().build()
        return htmlRenderer.render(document)
    }

    @JvmStatic
    fun registerAction(action: AnAction, actionId: String) {
        val actionManager = ActionManager.getInstance()
        if (actionManager.getAction(actionId) == null) {
            actionManager.registerAction(actionId, action, PluginId.getId("com.esprito.spring"))
        }
    }

    fun getSelectedEditorTextAndReset(project: Project): String? {
        val editor = FileEditorManager.getInstance(project)?.selectedTextEditor ?: return null

        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText
        if (!selectedText.isNullOrEmpty()) {
            selectionModel.removeSelection()
            val fileExtension = editor.virtualFile.extension ?: ""
            return String.format("```%s%n%s%n```", fileExtension, selectedText)
        }
        return null
    }

    fun getPromptWithSelectedFiles(text: String, files: List<Path>): String {
        val filesContext = files.joinToString("\n\n") {
            DEFAULT_REPEATABLE_CONTEXT
                .replace("{FILE_PATH}", it.absolutePathString())
                .replace(
                    "{FILE_CONTENT}", String.format(
                        "```%s%n%s%n```",
                        it.extension,
                        Files.readString(it).trim()
                    )
                )
        }
        return DEFAULT_PROMPT_TEMPLATE
            .replace("{REPEATABLE_CONTEXT}", filesContext)
            .replace("{QUESTION}", text);
    }
}
