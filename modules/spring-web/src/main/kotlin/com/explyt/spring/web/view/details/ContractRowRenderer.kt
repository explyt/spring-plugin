/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.details

import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

internal class ContractRowRenderer : ColoredTreeCellRenderer() {

    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean,
    ) {
        val contractRow = (value as? DefaultMutableTreeNode)?.userObject as? ContractRow ?: return
        icon = if (contractRow.uncertain) AllIcons.General.Warning else iconOf(contractRow.kind)
        append(contractRow.text, textAttributesOf(contractRow))
        contractRow.note?.let { append("  $it", SimpleTextAttributes.GRAYED_ATTRIBUTES) }
    }

    private fun textAttributesOf(row: ContractRow): SimpleTextAttributes = when {
        row.uncertain -> SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES
        row.kind == ContractRowKind.SECTION || row.kind == ContractRowKind.REQUEST ->
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES

        else -> SimpleTextAttributes.REGULAR_ATTRIBUTES
    }

    private fun iconOf(kind: ContractRowKind): Icon = when (kind) {
        ContractRowKind.REQUEST -> AllIcons.General.Web
        ContractRowKind.SECTION -> AllIcons.Nodes.Folder
        ContractRowKind.PARAMETER -> AllIcons.Nodes.Parameter
        ContractRowKind.MEDIA_TYPE -> AllIcons.FileTypes.Text
        ContractRowKind.TYPE -> AllIcons.Nodes.Class
        ContractRowKind.FIELD -> AllIcons.Nodes.Field
        ContractRowKind.VALUE -> AllIcons.Nodes.Property
        ContractRowKind.CALL -> AllIcons.Nodes.Method
        ContractRowKind.NOTE -> AllIcons.General.Information
    }
}
