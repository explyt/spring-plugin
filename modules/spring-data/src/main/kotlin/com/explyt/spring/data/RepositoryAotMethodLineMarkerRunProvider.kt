/*
 * Copyright © 2025 Explyt Ltd
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

package com.explyt.spring.data

import com.explyt.spring.data.SpringDataClasses.DATA_COMMON_MAVEN
import com.explyt.spring.data.util.SpringDataUtil
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.find.FindModel
import com.intellij.find.findInProject.FindInProjectManager
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.java.library.JavaLibraryUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiElement
import com.intellij.psi.util.InheritanceUtil
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUParentForIdentifier
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists


private const val SEARCH_AOT = "Explyt: Search AOT Implementation"

class RepositoryAotMethodLineMarkerRunProvider : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        //if (PluginIds.SPRING_DATA_JB.isEnabled()) return null
        val uMethod = getUParentForIdentifier(element) as? UMethod ?: return null
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return null
        if (!InheritanceUtil.isInheritor(uMethod.javaPsi.containingClass, SpringDataClasses.REPOSITORY)) return null
        if (!SpringDataUtil.isDataModule(module)) return null
        val dataVersion = JavaLibraryUtil.getLibraryVersion(module, DATA_COMMON_MAVEN) ?: return null
        if (VersionComparatorUtil.compare(dataVersion, "4.0") < 0) return null

        val aotFolder = findAotFolder(module) ?: findAotForParentModule(module) ?: return null

        return Info(
            AllIcons.Actions.Replace,
            arrayOf(EvaluateInDebugAction1(uMethod.name, aotFolder)),
            { SEARCH_AOT }
        )
    }

    private fun findAotForParentModule(module: Module): Path? {
        val moduleName = module.name
        val parentModuleName = if (moduleName.endsWith(".main"))
            moduleName.substringBeforeLast(".main") else return null
        val parentModule = ModuleManager.getInstance(module.project).findModuleByName(parentModuleName) ?: return null
        return findAotFolder(parentModule)
    }

    private fun findAotFolder(module: Module): Path? {
        val moduleRootManager = ModuleRootManager.getInstance(module)
        val contentRoots = moduleRootManager.excludeRoots.takeIf { it.isNotEmpty() } ?: return null
        return findAotFolder(contentRoots)
    }

    private fun findAotFolder(contentRoots: Array<VirtualFile>): Path? {
        for (virtualFile in contentRoots) {
            val nioPAth = virtualFile.toNioPathOrNull() ?: continue

            val aotMavenPath = nioPAth.resolve("spring-aot")
            if (aotMavenPath.exists()) return aotMavenPath

            val aotGradlePath = nioPAth.resolve("generated").resolve("aotSources")
            if (aotGradlePath.exists()) return aotGradlePath
        }
        return null
    }
}

private class EvaluateInDebugAction1(val stringToFind: String, val aotFolder: Path) :
    AnAction({ SEARCH_AOT }, AllIcons.Actions.Replace) {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val findModel = FindModel()
        findModel.stringToFind = stringToFind
        findModel.isProjectScope = false
        findModel.directoryName = aotFolder.absolutePathString()
        findModel.isCaseSensitive = true
        val component = e.inputEvent?.component ?: return
        val dataContext = DataManager.getInstance().getDataContext(component)
        FindInProjectManager.getInstance(project).findInProject(dataContext, findModel)
    }

}