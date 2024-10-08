package com.esprito.spring.core.externalsystem.action

import com.esprito.spring.core.externalsystem.utils.NativeBootUtils
import com.esprito.spring.core.externalsystem.view.nodes.SpringBeanViewNode
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.action.ExternalSystemAction
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys

class BeanNavigationAction : ExternalSystemAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val data = e.getData(ExternalSystemDataKeys.SELECTED_NODES)
        if (data?.size != 1) return
        val externalSystemNode = data[0] as? SpringBeanViewNode ?: return
        val beanData = externalSystemNode.dataNode.data

        val psiClass = NativeBootUtils.getPsiClassLocation(project, beanData) ?: return

        val psiElement = beanData.methodName
            ?.let { psiClass.findMethodsByName(beanData.methodName, false).firstOrNull() } ?: psiClass
        ApplicationManager.getApplication().invokeLater { psiElement.navigate(true) }
    }
}