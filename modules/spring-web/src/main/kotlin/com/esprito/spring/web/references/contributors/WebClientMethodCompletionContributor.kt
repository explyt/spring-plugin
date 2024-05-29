package com.esprito.spring.web.references.contributors

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.web.SpringWebClasses
import com.esprito.spring.web.service.beans.discoverer.SpringWebSearchService
import com.esprito.spring.web.util.SpringWebUtil
import com.esprito.util.EspritoAnnotationUtil.getStringValue
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.EditorModificationUtilEx
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.base.util.module

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
            val qualifierExpression =
                (parameters.position.parent as? PsiReferenceExpression)?.qualifierExpression ?: return
            if (qualifierExpression.type?.canonicalText != SpringWebClasses.WEB_CLIENT_RESPONSE_SPEC) return

            var uri: String? = null
            var method: String? = null
            var firstChild = qualifierExpression.firstChild
            while (firstChild != null && (uri == null || method == null)) {
                if (firstChild is PsiMethodCallExpression) {
                    val psiMethod = firstChild.resolveMethod()
                    val methodName = psiMethod?.name
                    if (methodName == "uri" && psiMethod.containingClass?.qualifiedName == SpringWebClasses.WEB_CLIENT_URI_SPEC) {
                        uri = (firstChild.argumentList.expressions.firstOrNull())?.getStringValue()
                    }
                    if (methodName in SpringWebUtil.REQUEST_METHODS) {
                        method = methodName?.uppercase()
                    }
                }

                firstChild = firstChild.firstChild
            }
            if (uri == null || method == null) return
            val module = parameters.position.module ?: return

            val endpointResults = SpringWebSearchService.getInstance(parameters.position.project)
                .getEndpointElements(uri.replace("\"", ""), module)
                .asSequence()
                .filter { it.requestMethods.contains(method) }
                .mapNotNull { it.psiElement as? PsiMethod }
                .mapNotNull { it.returnType as? PsiClassReferenceType }
                .mapNotNullTo(mutableSetOf()) { EndpointResult.of(it) }

            for (endpointResult in endpointResults) {
                result.addElement(
                    LookupElementBuilder.create("bodyTo${endpointResult.wrapperName}")
                        .withTailText("(${endpointResult.typeReferencePresentable})")
                        .withTypeText(endpointResult.returnType)
                        .withInsertHandler(WebClientMethodInsertHandler(endpointResult.typeReferenceCanonical))
                        .withIcon(AllIcons.Nodes.Method) //TODO: Custom Icon
                )
            }
        }
    }

    data class EndpointResult(
        val wrapperName: String,
        val typeReferencePresentable: String,
        val typeReferenceCanonical: String,
        val returnType: String
    ) {
        companion object {
            fun of(returnType: PsiClassReferenceType?): EndpointResult? {
                if (returnType == null) return null

                val wrapperName = getWrapperName(returnType) ?: return null
                val genericParameter = returnType.parameters.firstOrNull() ?: return null
                val classRefName = getClassRefName(genericParameter) ?: return null
                val typeReferencePreview = classRefName.presentable
                val typeReferenceCanonical = classRefName.canonical

                return EndpointResult(
                    wrapperName = wrapperName,
                    typeReferencePresentable = typeReferencePreview,
                    typeReferenceCanonical = typeReferenceCanonical,
                    returnType = returnType.presentableText
                )
            }

            private fun getWrapperName(psiType: PsiClassReferenceType): String? {
                return when (psiType.resolve()?.qualifiedName) {
                    "reactor.core.publisher.Mono" -> "Mono"
                    "reactor.core.publisher.Flux" -> "Flux"
                    else -> null
                }
            }

            private fun getClassRefName(psiType: PsiType): ClassRefName? {
                var typeToGetName = psiType

                if (psiType is PsiClassReferenceType) {
                    if (psiType.hasParameters()) {
                        val wrapper = psiType.resolve() ?: return null
                        val wrapperQn = wrapper.qualifiedName ?: return null
                        val parameter = psiType.parameters.first()

                        if (wrapperQn != SpringWebClasses.RESPONSE_ENTITY) {
                            val (parameterNameCanonical, parameterNamePresentable) = getParameterClassRefName(parameter)

                            return ClassRefName(
                                "new ${SpringCoreClasses.PARAMETERIZED_TYPE_REFERENCE}<$wrapperQn<$parameterNameCanonical>>(){}",
                                "new ParameterizedTypeReference<${wrapper.name}<$parameterNamePresentable>>(){}"
                            )
                        }

                        typeToGetName = parameter
                    }
                }

                return getParameterClassRefName(typeToGetName) + ".class"
            }


            private fun getParameterClassRefName(psiType: PsiType): ClassRefName {
                val typeToGetName = if (psiType is PsiWildcardType) psiType.extendsBound else psiType

                return ClassRefName.of(typeToGetName)
            }

            data class ClassRefName(val canonical: String, val presentable: String) {
                operator fun plus(suffix: String): ClassRefName {
                    return ClassRefName(canonical + suffix, presentable + suffix)
                }

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

    class WebClientMethodInsertHandler(private val type: String) :
        InsertHandler<LookupElement> {

        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            EditorModificationUtilEx.insertStringAtCaret(context.editor, "($type)")
            PsiDocumentManager.getInstance(context.project).commitDocument(context.document)

            val underCursor = context.file.findElementAt(context.tailOffset)?.parent ?: return

            JavaCodeStyleManager.getInstance(context.project).shortenClassReferences(underCursor)
        }

    }

}