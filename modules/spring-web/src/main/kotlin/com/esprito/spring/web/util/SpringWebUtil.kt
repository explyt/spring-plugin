package com.esprito.spring.web.util

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.util.SpringCoreUtil.isMapWithStringKey
import com.esprito.spring.web.SpringWebClasses
import com.esprito.util.EspritoAnnotationUtil.getBooleanValue
import com.esprito.util.EspritoAnnotationUtil.getStringValue
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isOptional
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

object SpringWebUtil {

    fun isSpringWebProject(project: Project): Boolean {
        return LibraryClassCache.searchForLibraryClass(
            project,
            SpringWebClasses.WEB_INITIALIZER
        ) != null
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

    fun collectPathVariables(psiMethod: PsiMethod): Collection<PathVariableInfo> {
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return emptyList()

        val annotatedParams = psiMethod.parameterList.parameters
            .filter { it.isMetaAnnotatedBy(SpringWebClasses.PATH_VARIABLE) }
        val mahPathVariable = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringWebClasses.PATH_VARIABLE)

        val pathVariableInfos = mutableListOf<PathVariableInfo>()
        for (param in annotatedParams) {
            val annotation = param.annotations.firstOrNull {
                mahPathVariable.contains(it)
            } ?: continue

            val paramType = param.type
            val isMap = paramType.isMapWithStringKey()
            val isOptional = !isMap && paramType.isOptional

            val isRequired = mahPathVariable.getAnnotationMemberValues(annotation, setOf("required"))
                .map { it.getBooleanValue() }
                .firstOrNull() ?: true

            val memberValues = mahPathVariable.getAnnotationMemberValues(annotation, setOf("value", "name"))
            if (memberValues.isEmpty()) {
                pathVariableInfos.add(
                    PathVariableInfo(
                        param.name,
                        param,
                        isRequired && !isOptional,
                        isMap
                    )
                )
            } else {
                memberValues.forEach {
                    val name = it.getStringValue() ?: return@forEach
                    pathVariableInfos.add(
                        PathVariableInfo(
                            name,
                            it,
                            isRequired && !isOptional,
                            isMap
                        )
                    )
                }
            }
        }
        return pathVariableInfos

    }

    fun simplifyUrl(urlPath: String): String {
        var result = if (urlPath.startsWith("/")) urlPath else "/$urlPath"

        result = result.replace(MultipleSlashes, "/")

        if (result.endsWith("/")) {
            result = result.substring(0, result.length - 1)
        }
        return result.split('?').first()
    }


    private val MultipleSlashes = Regex("//+")
    val NameInBracketsRx = Regex("""\{(?<name>[^{}]+)}""")

    data class PathVariableInfo(
        val name: String,
        val psiElement: PsiElement,
        val isRequired: Boolean,
        val isMap: Boolean
    )

    val REQUEST_METHODS = setOf("get", "head", "post", "put", "patch", "delete", "options", "trace")

}