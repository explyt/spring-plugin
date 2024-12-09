/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.web.util

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.util.SpringCoreUtil.isMapWithStringKey
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.SpringWebClasses.WEB_INITIALIZER
import com.explyt.spring.web.references.contributors.webClient.EndpointResult
import com.explyt.util.ExplytAnnotationUtil.getBooleanValue
import com.explyt.util.ExplytAnnotationUtil.getStringValue
import com.explyt.util.ExplytKotlinUtil.mapToList
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isOptional
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer
import com.intellij.json.psi.JsonProperty
import com.intellij.lang.Language
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.uast.*
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon

object SpringWebUtil {

    fun isSpringWebProject(project: Project): Boolean {
        return LibraryClassCache.searchForLibraryClass(
            project,
            SpringWebClasses.WEB_INITIALIZER
        ) != null
    }

    fun isSpringWebModule(psiElement: PsiElement): Boolean {
        return ModuleUtilCore.findModuleForPsiElement(psiElement)
            ?.let {
                JavaPsiFacade.getInstance(psiElement.project)
                    .findClass(WEB_INITIALIZER, it.moduleWithLibrariesScope) != null
            } == true
    }

    fun isSpringWebModule(module: Module): Boolean {
        return LibraryClassCache.searchForLibraryClass(
            module,
            SpringWebClasses.WEB_INITIALIZER
        ) != null
    }

    fun hasJakartaClasses(module: Module): Boolean {
        return LibraryClassCache.searchForLibraryClass(
            module,
            SpringWebClasses.JAKARTA_SERVLET_CONTEXT
        ) != null
    }

    fun getTypeFqn(returnType: PsiType?, language: Language): String {
        return if (returnType is PsiPrimitiveType) {
            returnType.boxedTypeName
        } else {
            EndpointResult.of(returnType as? PsiClassReferenceType, language, false)
                ?.typeReferenceCanonical
        } ?: "java.lang.String"
    }

    fun collectRequestParameters(psiMethod: PsiMethod): Collection<PathArgumentInfo> {
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return emptyList()

        val annotatedParams = psiMethod.parameterList.parameters
            .filter { it.isMetaAnnotatedBy(SpringWebClasses.REQUEST_PARAM) }
        val mahRequestParam = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringWebClasses.REQUEST_PARAM)

        val requestParamInfos = mutableListOf<PathArgumentInfo>()
        for (param in annotatedParams) {
            val annotation = param.annotations.firstOrNull {
                mahRequestParam.contains(it)
            } ?: continue

            val paramType = param.type
            val isMap = paramType.isMapWithStringKey()
            val isOptional = !isMap && paramType.isOptional
            val typeFqn = getTypeFqn(paramType, psiMethod.language)

            val isRequired = mahRequestParam.getAnnotationMemberValues(annotation, setOf("required"))
                .map { it.getBooleanValue() }
                .firstOrNull() ?: true

            val memberValues = mahRequestParam.getAnnotationMemberValues(annotation, setOf("value", "name"))
            if (memberValues.isEmpty()) {
                requestParamInfos.add(
                    PathArgumentInfo(
                        param.name,
                        param,
                        isRequired && !isOptional,
                        isMap,
                        typeFqn
                    )
                )
            } else {
                memberValues.forEach {
                    val name = it.getStringValue() ?: return@forEach
                    requestParamInfos.add(
                        PathArgumentInfo(
                            name,
                            it,
                            isRequired && !isOptional,
                            isMap,
                            typeFqn
                        )
                    )
                }
            }
        }
        return requestParamInfos
    }

    fun collectPathVariables(psiMethod: PsiMethod): Collection<PathArgumentInfo> {
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return emptyList()

        val annotatedParams = psiMethod.parameterList.parameters
            .filter { it.isMetaAnnotatedBy(SpringWebClasses.PATH_VARIABLE) }
        val mahPathVariable = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringWebClasses.PATH_VARIABLE)

        val pathVariableInfos = mutableListOf<PathArgumentInfo>()
        for (param in annotatedParams) {
            val annotation = param.annotations.firstOrNull {
                mahPathVariable.contains(it)
            } ?: continue

            val paramType = param.type
            val isMap = paramType.isMapWithStringKey()
            val isOptional = !isMap && paramType.isOptional
            val typeFqn = getTypeFqn(paramType, psiMethod.language)

            val isRequired = mahPathVariable.getAnnotationMemberValues(annotation, setOf("required"))
                .map { it.getBooleanValue() }
                .firstOrNull() ?: true

            val memberValues = mahPathVariable.getAnnotationMemberValues(annotation, setOf("value", "name"))
            if (memberValues.isEmpty()) {
                pathVariableInfos.add(
                    PathArgumentInfo(
                        param.name,
                        param,
                        isRequired && !isOptional,
                        isMap,
                        typeFqn
                    )
                )
            } else {
                memberValues.forEach {
                    val name = it.getStringValue() ?: return@forEach
                    pathVariableInfos.add(
                        PathArgumentInfo(
                            name,
                            it,
                            isRequired && !isOptional,
                            isMap,
                            typeFqn
                        )
                    )
                }
            }
        }
        return pathVariableInfos
    }

    fun getRequestBodyInfo(psiMethod: PsiMethod): PathArgumentInfo? {
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return null

        val annotatedParams = psiMethod.parameterList.parameters
            .filter { it.isMetaAnnotatedBy(SpringWebClasses.REQUEST_BODY) }
        val mahRequestBody = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringWebClasses.REQUEST_BODY)

        for (param in annotatedParams) {
            val annotation = param.annotations.firstOrNull {
                mahRequestBody.contains(it)
            } ?: continue

            val paramType = param.type
            val isMap = paramType.isMapWithStringKey()
            val isOptional = !isMap && paramType.isOptional
            val typeFqn = getTypeFqn(paramType, psiMethod.language)

            val isRequired = mahRequestBody.getAnnotationMemberValues(annotation, setOf("required"))
                .map { it.getBooleanValue() }
                .firstOrNull() ?: true

            return PathArgumentInfo(
                param.name,
                param,
                isRequired && !isOptional,
                isMap,
                typeFqn
            )
        }
        return null
    }

    fun getTargetRenderer(): PsiTargetPresentationRenderer<PsiElement> {
        return object : PsiTargetPresentationRenderer<PsiElement>() {
            override fun getIcon(element: PsiElement): Icon? {
                return element.language.associatedFileType?.icon ?: super.getIcon(element)
            }

            override fun getElementText(element: PsiElement): String {
                if (element is YAMLKeyValue)
                    return YAMLUtil.getConfigFullName(element).removePrefix("paths.")
                if (element is JsonProperty) {
                    return element.parentsOfType<JsonProperty>()
                        .mapToList { it.name }
                        .reversed()
                        .joinToString(".")
                        .removePrefix("paths.")
                }

                return element.getUastParentOfType<UMethod>()?.name ?: super.getElementText(element)
            }

            override fun getContainerText(element: PsiElement): String {
                return element.containingFile.name
            }
        }
    }

    fun getPathFromCallExpression(callExpression: UCallExpression): String {
        var path = ""

        var currentNode = callExpression as? UElement
        while (currentNode != null) {
            if (currentNode is UCallExpression) {
                if (currentNode.methodName == "nest") {
                    val currentNodeParent = currentNode.uastParent
                    if (currentNodeParent is UExpression) {
                        val qualifiedExpression = currentNodeParent.sourcePsi
                        if (qualifiedExpression is KtDotQualifiedExpression) {
                            path = getUri(qualifiedExpression, path)
                        }
                    }
                } else {
                    val argument = currentNode.valueArguments.firstOrNull()
                    if (argument is UPolyadicExpression) {
                        val operand = argument.operands.firstOrNull()
                        if (operand is ULiteralExpression) {
                            path = "$path${operand.value}"
                        }
                    } else if (argument is ULiteralExpression) {
                        path = "$path${argument.value}"
                    }
                }
            }
            currentNode = currentNode.uastParent
        }
        return path
    }

    private fun getUri(statement: KtDotQualifiedExpression, path: String): String {
        val receiver = statement.receiverExpression
        if (receiver is KtStringTemplateExpression) {
            val uri = receiver.entries.joinToString("") { it.text }
            return "$uri$path"
        }
        return path
    }

    fun simplifyUrl(urlPath: String): String {
        var result = if (urlPath.startsWith("/")) urlPath else "/$urlPath"

        result = result.replace(MultipleSlashes, "/")

        if (result.endsWith("/") && result.length > 1) {
            result = result.substring(0, result.length - 1)
        }
        return result.split('?').first()
    }

    fun getHttpMethodIndex(psiMethod: PsiMethod) =
        psiMethod.parameterList
            .parameters
            .indexOfFirst { it.name in HTTP_METHOD_NAMES }


    fun isEndpointMatches(endpoint: String, path: String): Boolean {
        return getRegexByUri(simplifyUrl(endpoint)).matches(simplifyUrl(path))
    }

    fun dropRegexesByUri() {
        endpointRegExByUri.clear()
    }

    private fun getRegexByUri(path: String): Regex {
        return endpointRegExByUri.computeIfAbsent(path) {
            val regex = path
                .replace(TEMPLATE_PARAM_REGEX, "[^/?]+")
                .replace(MULTIPLE_ASTERISKS, "*")
            Regex("^$regex(\\?.*)?\$")
        }
    }

    fun getUrlTemplateIndex(psiMethod: PsiMethod) =
        psiMethod.parameterList
            .parameters
            .indexOfFirst { it.name == URL_TEMPLATE && it.type.canonicalText == CommonClassNames.JAVA_LANG_STRING }

    private val MultipleSlashes = Regex("//+")
    val NameInBracketsRx = Regex("""\{(?<name>[^{}]+)}""")

    data class PathArgumentInfo(
        val name: String,
        val psiElement: PsiElement,
        val isRequired: Boolean,
        val isMap: Boolean,
        val typeFqn: String
    )

    val REQUEST_METHODS =
        setOf("get", "head", "post", "put", "patch", "delete", "options", "trace", "request", "multipart", "method")
    val REQUEST_METHODS_WITH_TYPE = listOf("MULTIPART", "REQUEST")
    val HTTP_METHOD_NAMES = setOf("httpMethod", "method")

    const val OPEN_API = "openapi"
    const val PATHS = "paths"
    const val OPENAPI_COMPONENTS = "components"
    const val OPENAPI_RESPONSES = "responses"
    const val OPENAPI_SCHEMAS = "schemas"
    const val OPENAPI_PARAMETERS = "parameters"
    private const val URL_TEMPLATE = "urlTemplate"

    private val endpointRegExByUri = ConcurrentHashMap<String, Regex>()
    private val TEMPLATE_PARAM_REGEX = Regex("\\{[^}]+}")
    private val MULTIPLE_ASTERISKS = Regex("\\*{2,}")

    const val OPENAPI_BOOLEAN = "type: boolean"
    const val OPENAPI_STRING = "type: string"
    const val OPENAPI_INT = "type: integer"

    const val OPENAPI_INT32 = """
              type: integer
              format: int32"""
    const val OPENAPI_INT64 = """
              type: integer
              format: int64"""
    const val OPENAPI_FLOAT = """
              type: number
              format: float"""
    const val OPENAPI_DOUBLE = """
              type: number
              format: double"""

    const val OPENAPI_UUID = """
              type: string
              format: uuid"""

    val simpleTypesMap = mapOf(
        "java.lang.String" to OPENAPI_STRING,
        "java.lang.Character" to OPENAPI_STRING,
        "kotlin.Char" to OPENAPI_STRING,

        "java.lang.Byte" to OPENAPI_INT,
        "kotlin.Byte" to OPENAPI_INT,
        "java.lang.Short" to OPENAPI_INT,
        "kotlin.Short" to OPENAPI_INT,

        "java.lang.Integer" to OPENAPI_INT32,
        "kotlin.Int" to OPENAPI_INT32,

        "java.lang.Long" to OPENAPI_INT64,
        "kotlin.Long" to OPENAPI_INT64,

        "java.lang.Float" to OPENAPI_FLOAT,
        "kotlin.Float" to OPENAPI_FLOAT,

        "java.lang.Double" to OPENAPI_DOUBLE,
        "kotlin.Double" to OPENAPI_DOUBLE,

        "java.lang.Boolean" to OPENAPI_BOOLEAN,
        "kotlin.Boolean" to OPENAPI_BOOLEAN,

        "java.util.UUID" to OPENAPI_UUID
    )

    val arrayTypes = listOf(
        "java.util.List",
        "kotlin.collections.List",
        "java.util.Collection",
        "kotlin.collections.Collection",
        "java.util.Set",
        "kotlin.collections.Set",
    )

}