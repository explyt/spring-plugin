package com.esprito.spring.web.references.contributors

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.web.SpringWebClasses
import com.esprito.spring.web.service.beans.discoverer.SpringWebEndpointsSearcher
import com.esprito.spring.web.util.SpringWebUtil
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.editor.EditorModificationUtilEx
import com.intellij.openapi.module.Module
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.psi.imports.addImport
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.uast.*

class WebClientMethodCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
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

            endpointsTypesForExpression(receiver, module)?.let { (_, endpointTypes) ->
                val endpointResults = endpointTypes
                    .mapNotNullTo(mutableSetOf()) { EndpointResult.of(it, parameters.position.language) }
                for (endpointResult in endpointResults) {
                    if (parameters.position.language == JavaLanguage.INSTANCE) {
                        addLookUpsJava(endpointResult, result)
                    } else if (parameters.position.language == KotlinLanguage.INSTANCE) {
                        addLookUpsKotlin(endpointResult, result)
                    }
                }
            }
        }

        private fun addLookUpsJava(endpointResult: EndpointResult, result: CompletionResultSet) {
            if (endpointResult.wrapperName == null) return

            result.addElement(
                LookupElementBuilder.create("bodyTo${endpointResult.wrapperName}")
                    .withTailText(createInsertPartJava(endpointResult.typeReferencePresentable, endpointResult.raw))
                    .withTypeText(endpointResult.returnType)
                    .withInsertHandler(
                        WebClientMethodInsertHandler(
                            createInsertPartJava(
                                endpointResult.typeReferenceCanonical,
                                endpointResult.raw
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
                var receiver = referenceExpression
                if (receiver?.getExpressionType()?.canonicalText != SpringWebClasses.WEB_CLIENT_RESPONSE_SPEC) return null

                var uri: String? = null
                var method: String? = null

                while (receiver != null && (uri == null || method == null)) {
                    val psiMethod = receiver.tryResolve() as? PsiMethod
                    if (psiMethod != null) {
                        val methodName = psiMethod.name
                        if (method == null && methodName in SpringWebUtil.REQUEST_METHODS) {
                            method = methodName.uppercase()
                        }

                        if (uri == null && methodName == "uri" && psiMethod.containingClass?.qualifiedName == SpringWebClasses.WEB_CLIENT_URI_SPEC) {
                            uri = (receiver.selector as? UCallExpression)
                                ?.getArgumentForParameter(0)
                                ?.evaluateString()
                        }
                    }
                    receiver = receiver.receiver as? UQualifiedReferenceExpression
                }
                if (uri == null || method == null) return null

                val endpointTypes = SpringWebEndpointsSearcher.getInstance(module.project)
                    .getAllEndpointElements(uri.replace("\"", ""), module)
                    .asSequence()
                    .filter { it.requestMethods.contains(method) }
                    .mapNotNull { it.psiElement as? PsiMethod }
                    .mapNotNull { it.returnType as? PsiClassReferenceType }

                return UriWithEndpointTypes(uri, endpointTypes)
            }

            data class UriWithEndpointTypes(val uri: String, val endpointTypes: Sequence<PsiClassReferenceType>)

            private val SEPARATORS_REGEX = Regex("[<>(),]")
        }

    }

    data class EndpointResult(
        val wrapperName: String?,
        val typeReferencePresentable: String,
        val typeReferenceCanonical: String,
        val returnType: String,
        val raw: Boolean
    ) {
        companion object {
            fun of(returnType: PsiClassReferenceType?, language: Language): EndpointResult? {
                if (returnType == null) return null

                val wrapperName = getWrapperName(returnType)
                if (wrapperName == null && language != KotlinLanguage.INSTANCE) return null

                val genericParameter = if (wrapperName == null) {
                    returnType
                } else {
                    returnType.parameters.firstOrNull() ?: return null
                }
                val classRefName = getClassRefName(genericParameter, language) ?: return null
                val typeReferencePreview = classRefName.presentable
                val typeReferenceCanonical = classRefName.canonical

                return EndpointResult(
                    wrapperName = wrapperName,
                    typeReferencePresentable = typeReferencePreview,
                    typeReferenceCanonical = typeReferenceCanonical,
                    returnType = returnType.presentableText,
                    classRefName.raw
                )
            }

            private fun getWrapperName(psiType: PsiClassReferenceType): String? {
                return when (psiType.resolve()?.qualifiedName) {
                    "reactor.core.publisher.Mono" -> "Mono"
                    "reactor.core.publisher.Flux" -> "Flux"
                    "kotlinx.coroutines.flow.Flow" -> "Flux"
                    else -> null
                }
            }

            private fun getClassRefName(psiType: PsiType, language: Language): ClassRefName? {
                if (psiType is PsiClassReferenceType) {
                    if (psiType.hasParameters()) {
                        val wrapper = psiType.resolve() ?: return null
                        val wrapperQn = wrapper.qualifiedName ?: return null
                        val parameter = psiType.parameters.first()

                        if (wrapperQn == SpringWebClasses.RESPONSE_ENTITY) {
                            return getClassRefName(parameter, language)
                        }

                        if (language == JavaLanguage.INSTANCE) {
                            val (parameterNameCanonical, parameterNamePresentable) = getParameterClassRefName(parameter)

                            return ClassRefName(
                                "new ${SpringCoreClasses.PARAMETERIZED_TYPE_REFERENCE}<$wrapperQn<$parameterNameCanonical>>(){}",
                                "new ParameterizedTypeReference<${wrapper.name}<$parameterNamePresentable>>(){}",
                                false
                            )
                        }
                    }
                }

                return getParameterClassRefName(psiType)
            }


            private fun getParameterClassRefName(psiType: PsiType): ClassRefName {
                val typeToGetName = if (psiType is PsiWildcardType) psiType.extendsBound else psiType

                return ClassRefName.of(typeToGetName)
            }

            data class ClassRefName(val canonical: String, val presentable: String, val raw: Boolean = true) {
                companion object {
                    fun of(psiType: PsiType): ClassRefName {
                        if (!psiType.canonicalText.contains(".")) {
                            return ClassRefName(CommonClassNames.JAVA_LANG_OBJECT, "Object")
                        }

                        return ClassRefName(psiType.canonicalText, psiType.presentableText)
                    }
                }
            }
        }
    }

    class WebClientMethodInsertHandler(private val type: String, private val toImport: Set<FqName> = setOf()) :
        InsertHandler<LookupElement> {

        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            EditorModificationUtilEx.insertStringAtCaret(context.editor, type)
            PsiDocumentManager.getInstance(context.project).commitDocument(context.document)

            val ktFile = context.file as? KtFile
            if (ktFile != null) {
                for (fqName in toImport) {
                    ktFile.addImport(fqName)
                }
                ktFile.addImport(toImport.first())
                return
            }

            val underCursor = context.file.findElementAt(context.tailOffset)?.parent ?: return
            JavaCodeStyleManager.getInstance(context.project).shortenClassReferences(underCursor)

        }

    }

}