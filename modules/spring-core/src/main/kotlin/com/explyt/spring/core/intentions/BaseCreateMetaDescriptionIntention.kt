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

package com.explyt.spring.core.intentions

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.SpringProperties.ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME
import com.explyt.spring.core.SpringProperties.META_INF
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.util.SourcesUtils
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo.CustomDiff
import com.intellij.icons.AllIcons
import com.intellij.json.JsonFileType
import com.intellij.json.psi.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.calcRelativeToProjectPath
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.util.IncorrectOperationException
import com.intellij.util.ObjectUtils
import com.intellij.util.Processor
import com.intellij.util.ThrowableRunnable
import java.io.IOException
import javax.swing.Icon

abstract class BaseCreateMetaDescriptionIntention : IntentionAction, HighPriorityAction, Iconable {

    override fun getFamilyName(): String {
        return SpringCoreBundle.message("explyt.spring.intention.create.property.description.familyName")
    }

    override fun getIcon(flags: Int): Icon {
        return SpringIcons.PropertyKey
    }

    override fun getText(): String {
        return SpringCoreBundle.message("explyt.spring.intention.create.property.description")
    }

    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        if (!isAvailable(file)) return false
        if (!SpringCoreUtil.isSpringBootProject(project)) return false

        val elementAtCaret = findElementAtCaret(editor, file) ?: return false
        return isAvailable(elementAtCaret)
    }

    protected fun findElementAtCaret(editor: Editor, file: PsiFile): PsiElement? {
        val offset = editor.caretModel.offset
        return file.findElementAt(offset)
    }

    protected abstract fun isAvailable(psiElement: PsiElement): Boolean

    protected abstract fun isAvailable(file: PsiFile): Boolean

    protected abstract fun rootArrayName(): String

    protected abstract fun getPropertyInfo(editor: Editor, file: PsiFile): PropertyInfo?

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val propertyInfo = getPropertyInfo(editor, file) ?: return

        return invoke(project, file, editor, propertyInfo)
    }

    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
        val propertyInfo = getPropertyInfo(editor, file) ?: return IntentionPreviewInfo.EMPTY

        return this.generatePreview(project, propertyInfo)
    }

    private fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        propertyInfo: PropertyInfo
    ) {
        val module = ModuleUtilCore.findModuleForPsiElement(propertyInfo.psiElement) ?: return
        val moduleName = module.name

        val resourceRoots = SourcesUtils.getResourceRoots(module).ifEmpty {
            project.invokeLater {
                Messages.showWarningDialog(
                    project,
                    SpringCoreBundle.message(
                        "explyt.spring.intention.create.property.description.no.roots",
                        moduleName
                    ),
                    SpringCoreBundle.message("explyt.spring.common.notifications.boot")
                )
            }
            return
        }

        val addKeyToExistingProcessor = Processor<JsonFile> { jsonFile ->
            addKey(project, jsonFile, propertyInfo)
            false
        }
        if (!processAdditionalMetadataFiles(resourceRoots, project, addKeyToExistingProcessor)) {
            return
        }

        if (editor == null) {
            createMetadataJson(project, resourceRoots[0], propertyInfo)
            return
        }

        val renderer = SimpleListCellRenderer.create { label: JBLabel, value: VirtualFile?, _: Int ->
            if (value == null) return@create

            label.setText(
                calcRelativeToProjectPath(value, project)
            )
            label.setIcon(AllIcons.Modules.ResourcesRoot)
        }

        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(resourceRoots)
            .setTitle(
                SpringCoreBundle.message(
                    "explyt.spring.intention.create.property.description.new.file",
                    propertyInfo.name
                )
            )
            .setAdText(
                SpringCoreBundle.message(
                    "explyt.spring.intention.create.property.not.found"
                )
            )
            .setRenderer(renderer)
            .setItemChosenCallback { selectedRoot ->
                if (selectedRoot == null) return@setItemChosenCallback
                createMetadataJson(project, selectedRoot, propertyInfo)
                DaemonCodeAnalyzer.getInstance(project).restart(file)
            }
            .setRequestFocus(true)
            .createPopup()
            .showInBestPositionFor(editor)
    }

    private fun processAllAdditionalMetadataFiles(
        resourceRoots: List<VirtualFile>,
        project: Project,
        processor: Processor<in PsiFile?>
    ): Boolean {
        for (root in resourceRoots) {
            val file = root.findFileByRelativePath(
                "$META_INF/$ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME"
            ) ?: continue
            val psiFile = PsiManager.getInstance(project).findFile(file)
            if (!processor.process(psiFile)) return false
        }
        return true
    }

    private fun processAdditionalMetadataFiles(
        resourceRoots: List<VirtualFile>,
        project: Project,
        processor: Processor<in JsonFile>
    ): Boolean {
        return processAllAdditionalMetadataFiles(resourceRoots, project) { psiFile: PsiFile? ->
            if (psiFile is JsonFile) {
                if (!processor.process(psiFile as JsonFile?)) return@processAllAdditionalMetadataFiles false
            }
            true
        }
    }

    protected fun generatePreview(project: Project, propertyInfo: PropertyInfo): IntentionPreviewInfo {
        val value = createKeyValue(JsonElementGenerator(project), propertyInfo)
        return CustomDiff(
            JsonFileType.INSTANCE,
            ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME,
            "", value.text
        )
    }

    private fun createMetadataJson(project: Project, selectedRoot: VirtualFile, propertyInfo: PropertyInfo) {
        WriteCommandAction.writeCommandAction(project)
            .withName(
                SpringCoreBundle.message(
                    "explyt.spring.intention.create.property.description.new.file",
                    propertyInfo.name
                )
            )
            .run<RuntimeException> {
                try {
                    val metaInf: VirtualFile =
                        VfsUtil.createDirectoryIfMissing(selectedRoot, META_INF)
                    var additionalMetaFile =
                        metaInf.findChild(ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME)
                    if (additionalMetaFile == null) {
                        additionalMetaFile =
                            metaInf.createChildData(
                                project,
                                ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME
                            )
                        VfsUtil.saveText(
                            additionalMetaFile,
                            ("""{ "${rootArrayName()}": [ ] }""")
                        )
                    }

                    val jsonFile: PsiFile? = PsiManager.getInstance(project).findFile(additionalMetaFile)
                    if (jsonFile is JsonFile) {
                        addKey(project, jsonFile, propertyInfo)
                    } else {
                        val fileName: String = additionalMetaFile.name

                        project.invokeLater {
                            Messages.showWarningDialog(
                                project,
                                SpringCoreBundle.message(
                                    "explyt.spring.intention.create.property.not.associated",
                                    fileName,
                                    JsonFileType.INSTANCE.name
                                ),
                                SpringCoreBundle.message("explyt.spring.common.notifications.boot")
                            )
                        }
                    }
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
    }

    private fun addKey(project: Project, additionalJson: JsonFile, propertyInfo: PropertyInfo) {
        if (!ReadonlyStatusHandler.ensureFilesWritable(project, additionalJson.virtualFile)) {
            return
        }

        val generator = JsonElementGenerator(project)
        val propertiesArray = findOrCreatePropertiesArray(generator, additionalJson)
        if (propertiesArray == null) {
            Messages.showWarningDialog(
                project,
                SpringCoreBundle.message(
                    "explyt.spring.intention.create.property.json.invalid",
                    additionalJson.virtualFile.path
                ),
                SpringCoreBundle.message("explyt.spring.common.notifications.boot")
            )
            return
        }

        WriteAction.run(ThrowableRunnable<IncorrectOperationException?> {
            val value: JsonObject = createKeyValue(generator, propertyInfo)
            val hasValues: Boolean = propertiesArray.valueList.isNotEmpty()
            if (hasValues) {
                propertiesArray.addBefore(generator.createComma(), propertiesArray.lastChild)
            }

            val added: JsonObject =
                propertiesArray.addBefore(value, propertiesArray.lastChild) as JsonObject

            CodeStyleManager.getInstance(project)
                .reformatText(additionalJson, 0, additionalJson.textLength)
            added.navigate(true)
        })
    }

    private fun findOrCreatePropertiesArray(generator: JsonElementGenerator, additionalJson: JsonFile): JsonArray? {
        val rootObject = ObjectUtils.tryCast(
            additionalJson.topLevelValue,
            JsonObject::class.java
        )
        if (rootObject == null) return null

        val propertiesRoot = rootObject.findProperty(rootArrayName())
            ?: return WriteAction.compute<JsonArray?, IncorrectOperationException?> {
                val propertiesProperty: JsonProperty =
                    generator.createProperty(rootArrayName(), "[]")
                if (rootObject.propertyList.isNotEmpty()) {
                    rootObject.addBefore(generator.createComma(), rootObject.lastChild)
                }

                val propertiesAdded: JsonProperty = rootObject.addBefore(
                    propertiesProperty,
                    rootObject.lastChild
                ) as JsonProperty
                propertiesAdded.value as? JsonArray
            }

        return propertiesRoot.value as? JsonArray
    }

    protected open fun createKeyValue(generator: JsonElementGenerator, propertyInfo: PropertyInfo): JsonObject {
        val (keyName, type) = propertyInfo
        return generator.createValue(
            """
            {
              "name": "$keyName",
              "type": "$type",
              "description": "Description for $keyName."
            }       
            """.trimIndent()
        )
    }

    data class PropertyInfo(
        val name: String,
        val type: String,
        val psiElement: PsiElement
    )

    companion object {
        private fun Project.invokeLater(operation: () -> Unit) {
            ApplicationManager.getApplication()
                .invokeLater(operation, this.disposed)
        }
    }

}