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
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.ui.SimpleTextAttributes
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class EndpointCellRenderer : NodeRenderer() {
    override fun customizeCellRenderer(
        tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean
    ) {

        val endpointElement = (value as? DefaultMutableTreeNode)?.userObject as? EndpointElement

        if (endpointElement == null) {
            super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus)
        } else {
            append(endpointElement.requestMethods.toString(), SimpleTextAttributes.GRAY_ATTRIBUTES)
            append(endpointElement.path, SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES, true)
        }
    }

}
