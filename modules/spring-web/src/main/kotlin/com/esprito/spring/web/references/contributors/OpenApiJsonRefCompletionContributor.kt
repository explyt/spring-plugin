package com.esprito.spring.web.references.contributors

import com.esprito.spring.web.references.OpenApiJsonInnerReference.Companion.getProperty
import com.esprito.spring.web.references.contributors.providers.OpenApiVersionCompletionProvider
import com.esprito.spring.web.util.PlatformPatternUtils
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.util.childrenOfType
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class OpenApiJsonRefCompletionContributor : CompletionContributor() {

    init {
        extend(
            null,
            psiElement()
                .withParent(
                    PlatformPatternUtils.openApiJsonInnerRef()
                ),
            OpenApiJsonRefCompletionProvider()
        )

        extend(CompletionType.BASIC, psiElement(), OpenApiVersionCompletionProvider())
    }


    class OpenApiJsonRefCompletionProvider : CompletionProvider<CompletionParameters>() {

        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val literal = parameters.position.parent as? JsonStringLiteral ?: return
            val jsonFile = literal.containingFile as? JsonFile ?: return

            for (component in componentsToScan) {
                val jsonPropertyValue = jsonFile
                    .getProperty(component.split('/'))
                    ?.getChildOfType<JsonObject>() ?: continue

                val prefix = "#/$component/"
                result.addAllElements(
                    jsonPropertyValue.childrenOfType<JsonProperty>()
                        .map {
                            LookupElementBuilder.create("$prefix${it.name}")
                        }
                )
            }
        }

        companion object {
            private val componentsToScan = setOf("components/parameters", "components/responses", "components/schemas")
        }

    }

}