package com.explyt.spring.core.properties

import com.explyt.spring.core.util.PropertyUtil
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import com.intellij.icons.AllIcons

class ClassReferencePropertyRenderer : LookupElementRenderer<LookupElement>() {

    override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
        val lookupString = element.lookupString
        val className = PropertyUtil.getClassNameByQualifiedName(lookupString)
        val packageName = PropertyUtil.getPackageNameByQualifiedName(lookupString)
        presentation.itemText = className
        presentation.setTailText(" ($packageName)", true)
        presentation.icon = AllIcons.Nodes.Class
    }

}