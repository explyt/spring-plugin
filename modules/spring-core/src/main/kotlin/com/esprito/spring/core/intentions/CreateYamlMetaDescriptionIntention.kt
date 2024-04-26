package com.esprito.spring.core.intentions

import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.util.PropertyUtil
import com.esprito.spring.core.util.SpringCoreUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence

class CreateYamlMetaDescriptionIntention : BaseCreateMetaDescriptionIntention() {

    override fun isAvailable(psiElement: PsiElement): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return false

        val parent = psiElement.parent as? YAMLKeyValue ?: return false
        if (parent.key !== psiElement) return false

        val value = parent.value ?: return false
        if (value !is YAMLScalar && value !is YAMLSequence) return false

        val keyName = YAMLUtil.getConfigFullName(parent)

        return SpringConfigurationPropertiesSearch.getInstance(psiElement.project)
            .findProperty(module, keyName) == null
    }

    override fun isAvailable(file: PsiFile): Boolean {
        if (!SpringCoreUtil.isConfigurationPropertyFile(file)) return false
        return YAMLLanguage.INSTANCE == file.language
    }

    override fun rootArrayName(): String = "properties"

    override fun getPropertyInfo(editor: Editor, file: PsiFile): PropertyInfo? {
        val parentProperty = findElementAtCaret(editor, file)?.parent as? YAMLKeyValue ?: return null
        val keyName = YAMLUtil.getConfigFullName(parentProperty)
        val type = PropertyUtil.guessTypeFromValue(parentProperty.valueText)

        return PropertyInfo(
            name = keyName,
            type = type,
            parentProperty
        )
    }


}