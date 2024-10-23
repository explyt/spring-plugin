package com.esprito.spring.web.references.contributors

import com.esprito.spring.web.references.contributors.providers.OpenApiVersionCompletionProvider
import com.esprito.spring.web.util.PlatformPatternUtils
import com.esprito.util.EspritoKotlinUtil.mapToList
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.util.childrenOfType
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar

class OpenApiYamlRefCompletionContributor : CompletionContributor() {

    init {
        extend(
            null,
            psiElement()
                .withParent(
                    PlatformPatternUtils.openApiYamlInnerRef()
                ),
            OpenApiYamlRefCompletionProvider()
        )

        extend(CompletionType.BASIC, psiElement(), OpenApiVersionCompletionProvider())
    }


    class OpenApiYamlRefCompletionProvider : CompletionProvider<CompletionParameters>() {

        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val yamlScalar = parameters.position.parent as? YAMLScalar ?: return
            val yamlFile = yamlScalar.containingFile as? YAMLFile ?: return

            val componentsKey = YAMLUtil.getQualifiedKeyInFile(yamlFile, KEY_COMPONENTS) ?: return
            val componentsKeyValues = componentsKey.childrenOfType<YAMLMapping>().asSequence()
                .flatMap { it.keyValues }
                .filter { it.keyText in componentsToScan }

            for (componentKeyValue in componentsKeyValues) {
                val prefix = "#/$KEY_COMPONENTS/${componentKeyValue.keyText}/"

                result.addAllElements(
                    componentKeyValue.childrenOfType<YAMLMapping>().asSequence()
                        .flatMap { it.keyValues }
                        .mapToList {
                            LookupElementBuilder.create("$prefix${it.keyText}")
                        }
                )
            }
        }

        companion object {
            const val KEY_COMPONENTS = "components"
            private val componentsToScan = setOf("parameters", "responses", "schemas")
        }

    }

}