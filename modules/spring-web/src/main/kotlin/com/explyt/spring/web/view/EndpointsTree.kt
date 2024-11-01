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

package com.explyt.spring.web.view

import com.explyt.spring.web.loader.EndpointElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.pom.Navigatable
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.treeStructure.treetable.TreeTable
import com.intellij.util.ui.tree.TreeUtil
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.tree.TreePath

class EndpointsTree(val project: Project, model: EndpointsTreeModel) : TreeTable(model) {

    init {
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode != KeyEvent.VK_ENTER) return
                if (tree.selectionCount == 0) return

                tree.selectionPath?.let {
                    navigateToEndpoint(it)
                }
            }
        })

        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                navigateToEndpoint(tree.getClosestPathForLocation(event.x, event.y))
                return false
            }
        }.installOn(this)
    }

    private fun navigateToEndpoint(nodePath: TreePath?) {
        ToolWindowManager.getInstance(project).invokeLater {
            (TreeUtil.getLastUserObject(EndpointElement::class.java, nodePath)
                ?.psiElement?.navigationElement as? Navigatable)?.navigate(true)
        }
    }

}