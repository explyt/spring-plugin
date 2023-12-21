package com.esprito.spring.web.inspections

import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.esprito.spring.web.SpringWebBundle
import com.esprito.spring.web.SpringWebClasses.REQUEST_MAPPING
import com.esprito.spring.web.inspections.quickfix.AddPathVariableQuickFix
import com.esprito.spring.web.util.SpringWebUtil
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.toSourcePsi
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.*
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UMethod

class SpringOmittedPathVariableParameterInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkMethod(
        method: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val psiMethod = method.javaPsi
        if (!psiMethod.isMetaAnnotatedBy(REQUEST_MAPPING)) return null
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return null

        val urlPathParams = collectUrlPathParams(module, psiMethod)
        val pathVariableInfos = SpringWebUtil.collectPathVariables(psiMethod)

        val problems = mutableListOf<ProblemDescriptor>()

        for (pathVariableInfo in pathVariableInfos) {
            if (!pathVariableInfo.isMap
                && pathVariableInfo.isRequired
                && urlPathParams.any { urlPathParam ->
                    urlPathParam.namesWithRanges.none {
                        it.name == pathVariableInfo.name
                    }
                }
            ) {
                val pathVariableSourcePsi = pathVariableInfo.psiElement.toSourcePsi() ?: continue
                problems += manager.createProblemDescriptor(
                    pathVariableSourcePsi,
                    pathVariableSourcePsi.getHighlightRange(),
                    SpringWebBundle.message("esprito.spring.web.inspection.pathVariable"),
                    ProblemHighlightType.WARNING,
                    isOnTheFly,
                )
            }
        }

        if (pathVariableInfos.none { it.isMap }) {
            for (urlPathParam in urlPathParams) {
                for (nameAndRange in urlPathParam.namesWithRanges) {
                    if (pathVariableInfos.none {
                            it.name == nameAndRange.name
                        }
                    ) {
                        val urlPathParamSourcePsi = urlPathParam.element.toSourcePsi() ?: continue
                        val fixes = mutableListOf<LocalQuickFix>()
                        if (urlPathParam.element.isInJavaFile()) {
                            fixes.add(AddPathVariableQuickFix(psiMethod, nameAndRange.name))
                        }

                        problems += manager.createProblemDescriptor(
                            urlPathParamSourcePsi,
                            nameAndRange.range,
                            SpringWebBundle.message("esprito.spring.web.inspection.pathVariable"),
                            ProblemHighlightType.WARNING,
                            isOnTheFly,
                            *fixes.toTypedArray()
                        )
                    }
                }
            }
        }

        return problems.toTypedArray()
    }

    private fun collectUrlPathParams(module: Module, psiMethod: PsiMethod): MutableList<RefInfo> {
        val mahRequestMapping = MetaAnnotationsHolder.of(module, REQUEST_MAPPING)
        val urlPaths = mahRequestMapping.getAnnotationMemberValues(psiMethod, setOf("value", "path"))

        val urlPathParams = mutableListOf<RefInfo>()
        for (memberValue in urlPaths) {
            val urlPath = AnnotationUtil.getStringAttributeValue(memberValue) ?: continue
            val namesWithRanges = SpringWebUtil.NameInBracketsRx.findAll(urlPath)
                .mapNotNull { it.groups["name"] }
                .mapTo(mutableListOf()) {
                    val range = if (memberValue.isInJavaFile()) {
                        TextRange(it.range.first + 1, it.range.last + 2)
                    } else {
                        TextRange(it.range.first, it.range.last + 1)
                    }
                    NameWithRange(it.value, range)
                }
            urlPathParams.add(RefInfo(memberValue, namesWithRanges))
        }
        return urlPathParams
    }

    private fun PsiElement.isInJavaFile() = containingFile.fileType is JavaFileType

    private data class RefInfo(val element: PsiElement, val namesWithRanges: List<NameWithRange>)
    private data class NameWithRange(val name: String, val range: TextRange)
}