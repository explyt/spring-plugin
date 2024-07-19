package com.esprito.spring.web.references.contributors.webClient

import com.esprito.spring.web.SpringWebClasses
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.module.Module
import com.intellij.patterns.PlatformPatterns.*
import com.intellij.patterns.StandardPatterns.not
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.getUastParentOfType

class WebClientMethodCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            and(
                psiElement(),
                not(psiComment())
            ),
            WebClientMethodCompletionProvider()
        )
    }

    class WebClientMethodCompletionProvider : CompletionProvider<CompletionParameters>() {

        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val receiver: UQualifiedReferenceExpression =
                parameters.position.getUastParentOfType<UQualifiedReferenceExpression>()
                    ?.receiver as? UQualifiedReferenceExpression
                    ?: return

            val module = parameters.position.module ?: return
            val language = parameters.position.language
            val isJava = language == JavaLanguage.INSTANCE

            endpointsTypesForExpression(receiver, module)?.let { (_, endpointTypes) ->
                val endpointResults = endpointTypes
                    .mapNotNullTo(mutableSetOf()) { EndpointResult.of(it, language, isJava) }
                for (endpointResult in endpointResults) {
                    if (isJava) {
                        addLookUpsJava(endpointResult, result)
                    } else if (language == KotlinLanguage.INSTANCE) {
                        addLookUpsKotlin(endpointResult, result)
                    }
                }
            }
        }

        private fun addLookUpsJava(endpointResult: EndpointResult, result: CompletionResultSet) {
            if (endpointResult.wrapperName == null) return

            result.addElement(
                LookupElementBuilder.create("bodyTo${endpointResult.wrapperName}")
                    .withTailText(createInsertPartJava(endpointResult.typeReferencePresentable, endpointResult.isRaw))
                    .withTypeText(endpointResult.returnType)
                    .withInsertHandler(
                        WebClientMethodInsertHandler(
                            createInsertPartJava(
                                endpointResult.typeReferenceCanonical,
                                endpointResult.isRaw
                            )
                        )
                    )
                    .withIcon(AllIcons.Nodes.Method)
            )
        }

        private fun createInsertPartJava(type: String, raw: Boolean): String {
            return "($type${if (raw) ".class" else ""})"
        }

        private fun addLookUpsKotlin(endpointResult: EndpointResult, result: CompletionResultSet) {
            val wrapperName = endpointResult.wrapperName ?: "Mono"

            val toImport =
                endpointResult.typeReferenceCanonical
                    .replace(SEPARATORS_REGEX, " ")
                    .split(' ')
                    .filter { it.isNotBlank() }
                    .map { FqName(it) }
                    .toSet()

            result.addElement(
                LookupElementBuilder.create("bodyTo$wrapperName")
                    .withTailText("<${endpointResult.typeReferencePresentable}>()")
                    .withTypeText(endpointResult.returnType)
                    .withInsertHandler(
                        WebClientMethodInsertHandler(
                            "<${endpointResult.typeReferencePresentable}>()",
                            toImport + FqName("org.springframework.web.reactive.function.client.bodyTo$wrapperName")
                        )
                    )
                    .withIcon(AllIcons.Nodes.Method)
            )

            result.addElement(
                LookupElementBuilder.create("awaitBody")
                    .withTailText("<${endpointResult.typeReferencePresentable}>()")
                    .withTypeText(endpointResult.returnType)
                    .withInsertHandler(
                        WebClientMethodInsertHandler(
                            "<${endpointResult.typeReferencePresentable}>()",
                            toImport + FqName("org.springframework.web.reactive.function.client.awaitBody")
                        )
                    )
                    .withIcon(AllIcons.Nodes.Method)
            )
        }

        companion object {
            fun endpointsTypesForExpression(
                referenceExpression: UQualifiedReferenceExpression?,
                module: Module
            ): UriWithEndpointTypes? {
                return UriWithEndpointTypes.endpointsTypesForExpression(
                    referenceExpression,
                    SpringWebClasses.WEB_CLIENT_RESPONSE_SPEC,
                    SpringWebClasses.WEB_CLIENT_URI_SPEC,
                    module
                )
            }

            private val SEPARATORS_REGEX = Regex("[<>(),]")
        }

    }

}