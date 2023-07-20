package com.esprito.spring.core.completion.properties

import com.esprito.spring.core.util.SpringCoreUtil
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.properties.IProperty
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

class SpringPropertiesCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), SpringPropertyCompletionProvider())
    }

    class SpringPropertyCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val position = parameters.position as? PropertyKeyImpl ?: return
            if (position.parent !is IProperty
                || !SpringCoreUtil.isPropertyFile(position.containingFile)
            ) {
                return
            }

            val module = ModuleUtilCore.findModuleForPsiElement(position) ?: return
            if (!SpringCoreUtil.isSpringProject(module)) {
                return
            }

            val cursor = parameters.offset
            val keyStart = position.textOffset

            val key = position.text.substring(0, cursor - keyStart)

            val properties = SpringConfigurationPropertiesSearch.getInstance(module.project)
                .getAllProperties(module)

            for (property in properties) {
                result.withPrefixMatcher(key).addElement(
                    LookupElementBuilder.create(property, property.name).withRenderer(PropertyRenderer())
                )
            }
        }
    }
}
