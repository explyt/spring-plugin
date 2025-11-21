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

package com.explyt.spring.core.autoconfigure.inspection

import com.explyt.base.LibraryClassCache
import com.explyt.inspection.SpringBaseLocalInspectionTool
import com.explyt.plugin.PluginIds
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses.BOOT_AUTO_CONFIGURATION
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.SpringProperties.FACTORIES_ENABLE_AUTO_CONFIGURATION
import com.explyt.spring.core.autoconfigure.language.FactoriesFileType
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.intellij.codeInsight.daemon.quickFix.CreateFilePathFix
import com.intellij.codeInsight.daemon.quickFix.NewFileLocation
import com.intellij.codeInsight.daemon.quickFix.TargetDirectory
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.ide.actions.OpenFileAction
import com.intellij.lang.properties.IProperty
import com.intellij.lang.properties.PropertiesBundle
import com.intellij.lang.properties.PropertiesQuickFixFactory
import com.intellij.lang.properties.psi.PropertiesElementFactory
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.Property
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.DelimitedListProcessor
import com.intellij.openapi.vfs.findDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.codeinsight.utils.findExistingEditor
import java.util.function.Supplier
import javax.swing.Icon

class EnableAutoConfigureSpringFactoryInspection : SpringBaseLocalInspectionTool() {

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        if (PluginIds.SPRING_BOOT_JB.isEnabled()) return emptyArray()

        if (!FactoriesFileType.isMyFileType(file.virtualFile)) return ProblemDescriptor.EMPTY_ARRAY
        val propertiesFile = file as? PropertiesFile ?: return ProblemDescriptor.EMPTY_ARRAY
        val module = ModuleUtilCore.findModuleForFile(file) ?: return ProblemDescriptor.EMPTY_ARRAY
        LibraryClassCache.searchForLibraryClass(module, BOOT_AUTO_CONFIGURATION) ?: return ProblemDescriptor.EMPTY_ARRAY

        val property = propertiesFile.findPropertyByKey(FACTORIES_ENABLE_AUTO_CONFIGURATION)
            ?: return ProblemDescriptor.EMPTY_ARRAY
        val values = getValues(property.value)

        val problems = mutableListOf<ProblemDescriptor>()
        if (values.isEmpty()) {
            val removePropertyLocalFix = RemovePropertyLocalFix()
            problems += manager.createProblemDescriptor(
                property.psiElement,
                removePropertyLocalFix.familyName,
                isOnTheFly,
                arrayOf(removePropertyLocalFix),
                ProblemHighlightType.WARNING
            )
            return problems.toTypedArray()
        }
        val autoConfigurationImportsFile = propertiesFile.virtualFile.parent
            .findDirectory(SpringProperties.SPRING)
            ?.findChild(SpringProperties.AUTOCONFIGURATION_IMPORTS)

        if (autoConfigurationImportsFile != null) {
            val movePropertyFix = MovePropertyFix(values)
            problems += manager.createProblemDescriptor(
                property.psiElement,
                SpringCoreBundle.message("explyt.spring.inspection.properties.auto.configuration.move.fix"),
                isOnTheFly,
                arrayOf(movePropertyFix),
                ProblemHighlightType.WARNING
            )
            return problems.toTypedArray()
        }

        val content = values.joinToString(System.lineSeparator())
        val directory = TargetDirectory(propertiesFile.parent, arrayOf(SpringProperties.SPRING))
        val location = NewFileLocation(listOf(directory), SpringProperties.AUTOCONFIGURATION_IMPORTS)
        val fix = CreateFilePathSpringFactoriesFix(property.psiElement, location) { content }

        problems += manager.createProblemDescriptor(
            property.psiElement,
            SpringCoreBundle.message("explyt.spring.inspection.properties.auto.configuration.problem"),
            isOnTheFly,
            arrayOf(fix),
            ProblemHighlightType.WARNING
        )
        return problems.toTypedArray()
    }

    private fun getValues(propertyValue: String?): List<String> {
        propertyValue ?: return emptyList()
        val values = ArrayList<String>()
        object : DelimitedListProcessor(SpringProperties.PROPERTY_VALUE_DELIMITERS) {
            override fun processToken(start: Int, end: Int, delimitersOnly: Boolean) {
                values.add(propertyValue.substring(start, end))
            }
        }.processText(propertyValue)
        return values.filter { it.isNotEmpty() }
    }
}

private class CreateFilePathSpringFactoriesFix(
    psiElement: PsiElement, newFileLocation: NewFileLocation, fileTextSupplier: Supplier<String>
) : CreateFilePathFix(psiElement, newFileLocation, fileTextSupplier) {

    init {
        myIsAvailable = true
    }

    override fun invoke(
        project: Project, file: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement
    ) {
        super.invoke(project, file, editor, startElement, endElement)
        if (startElement is PropertyImpl) startElement.delete()
    }

    override fun getDescription(itemIcon: Icon) = super.getDescription(SpringIcons.SpringFactories)
}

private class MovePropertyFix(private val movedValues: List<String>) : LocalQuickFix {
    override fun getFamilyName(): String {
        return SpringCoreBundle.message("explyt.spring.inspection.properties.auto.configuration.move.fix")
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        StatisticService.getInstance().addActionUsage(StatisticActionId.QUICK_FIX_FACTORIES_MOVE_TO_IMPORT)
        val autoConfigurationImportsFile = descriptor.psiElement.containingFile.virtualFile.parent
            .findDirectory(SpringProperties.SPRING)
            ?.findChild(SpringProperties.AUTOCONFIGURATION_IMPORTS) ?: return
        val autoConfigurationPropertyFile = PsiManager.getInstance(project)
            .findFile(autoConfigurationImportsFile) as? PropertiesFile ?: return

        val existedKeys = autoConfigurationPropertyFile.properties.asSequence().mapNotNull { it.key }.toSet()
        val valuesToAdd = movedValues.filter { !existedKeys.contains(it) }.joinToString(System.lineSeparator())
        val dummyPropertiesFile = PropertiesElementFactory.createPropertiesFile(project, valuesToAdd)
        val lastProperty: IProperty? = if (autoConfigurationPropertyFile.properties.isEmpty()) null else
            autoConfigurationPropertyFile.properties.last()
        dummyPropertiesFile.properties.forEach { autoConfigurationPropertyFile.addPropertyAfter(it, lastProperty) }
        OpenFileAction.openFile(autoConfigurationImportsFile, project)
        if (descriptor.psiElement is PropertyImpl) descriptor.psiElement.delete()
    }
}

class RemovePropertyLocalFix : LocalQuickFix {

    override fun getFamilyName() = PropertiesBundle.message("remove.property.intention.text")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        StatisticService.getInstance().addActionUsage(StatisticActionId.QUICK_FIX_FACTORIES_REMOVE_PROPERTY)
        val element = descriptor.psiElement
        val property = PsiTreeUtil.getParentOfType(element, Property::class.java, false) ?: return
        try {
            val editor = element.containingFile.findExistingEditor() ?: return
            PropertiesQuickFixFactory.getInstance().createRemovePropertyFix(property)
                .invoke(project, editor, property.containingFile)
        } catch (e: IncorrectOperationException) {
        }
    }
}