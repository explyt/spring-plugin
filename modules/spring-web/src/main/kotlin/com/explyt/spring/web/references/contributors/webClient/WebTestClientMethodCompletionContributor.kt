package com.explyt.spring.web.references.contributors.webClient

import com.explyt.spring.web.SpringWebClasses
import com.explyt.util.ExplytKotlinUtil
import com.explyt.util.ExplytKotlinUtil.mapToSet
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.module.Module
import com.intellij.patterns.PlatformPatterns.*
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.getUastParentOfType

class WebTestClientMethodCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            and(
                psiElement(),
                not(psiComment())
            ),
            WebTestClientMethodCompletionProvider()
        )
    }

    class WebTestClientMethodCompletionProvider : CompletionProvider<CompletionParameters>() {

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

            endpointsTypesForExpression(receiver, module)?.let { (_, endpointTypes) ->
                val endpointResults = endpointTypes
                    .mapNotNullTo(mutableSetOf()) {
                        EndpointResult.of(
                            it,
                            language,
                            true
                        )
                    }
                for (endpointResult in endpointResults) {
                    if (language == JavaLanguage.INSTANCE) {
                        addLookUpsJava(endpointResult, result)
                    } else if (language == KotlinLanguage.INSTANCE) {
                        addLookUpsKotlin(endpointResult, result)
                    }
                }
            }

        }

        private fun addLookUpsJava(
            endpointResult: EndpointResult,
            result: CompletionResultSet
        ) {
            if (endpointResult.wrapperName == null) return

            val methodSuffix = if (endpointResult.wrapperName == "Flux") "List" else ""

            result.addElement(
                LookupElementBuilder.create("expectBody$methodSuffix")
                    .withTailText(
                        createInsertPartJava(
                            endpointResult.typeReferencePresentable,
                            endpointResult.isRaw
                        )
                    )
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

        private fun addLookUpsKotlin(
            endpointResult: EndpointResult,
            result: CompletionResultSet
        ) {
            val wrapperName = endpointResult.wrapperName ?: "Mono"
            val methodSuffix = if (wrapperName != "Mono") "List" else ""

            val toImport =
                endpointResult.typeReferenceCanonical
                    .replace("object : ", "")
                    .replace(SEPARATORS_REGEX, " ")
                    .split(' ').asSequence()
                    .filter { it.isNotBlank() }
                    .map { ExplytKotlinUtil.toKotlinType(it) }
                    .mapToSet { FqName(it) }

            val insertPartKotlin = createInsertPartKotlin(
                endpointResult.typeReferencePresentable,
                endpointResult.isRaw
            )

            result.addElement(
                LookupElementBuilder.create("expectBody$methodSuffix")
                    .withTailText(insertPartKotlin)
                    .withTypeText(endpointResult.returnType)
                    .withInsertHandler(
                        WebClientMethodInsertHandler(
                            insertPartKotlin,
                            toImport
                        )
                    )
                    .withIcon(AllIcons.Nodes.Method)
            )
        }

        private fun createInsertPartJava(type: String, raw: Boolean): String {
            return "($type${if (raw) ".class" else ""})"
        }

        private fun createInsertPartKotlin(type: String, raw: Boolean): String {
            return "($type${if (raw) "::class.java" else ""})"
        }


        companion object {
            fun endpointsTypesForExpression(
                referenceExpression: UQualifiedReferenceExpression?,
                module: Module
            ): UriWithEndpointTypes? {
                return UriWithEndpointTypes.endpointsTypesForExpression(
                    referenceExpression,
                    SpringWebClasses.WEB_TEST_CLIENT_RESPONSE_SPEC,
                    SpringWebClasses.WEB_TEST_CLIENT_URI_SPEC,
                    module
                )
            }

            private val SEPARATORS_REGEX = Regex("[<>(),{}]")
        }

    }

}