package com.esprito.spring.data.inspection

import com.esprito.base.LibraryClassCache
import com.esprito.spring.data.SpringDataBundle.message
import com.esprito.spring.data.SpringDataClasses
import com.esprito.spring.data.util.SpringDataRepositoryUtil
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType
import org.springframework.data.repository.query.parser.Part
import org.springframework.data.repository.query.parser.PartTree
import org.springframework.data.repository.query.parser.domain.PropertyPath


class SpringDataMethodNameInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkMethod(
        method: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val javaMethodPsi = method.javaPsi
        val uClass = method.getParentOfType<UClass>() ?: return emptyArray()
        val typeParams = SpringDataRepositoryUtil.substituteRepositoryTypes(uClass.javaPsi) ?: return emptyArray()
        if (AnnotationUtil.isAnnotated(javaMethodPsi, SpringDataClasses.QUERY, AnnotationUtil.CHECK_HIERARCHY)) {
            return emptyArray()
        }

        val module = ModuleUtilCore.findModuleForPsiElement(javaMethodPsi)
        if (getPredefinedSpringDataMethods(module).contains(method.name)) return emptyArray()

        val methodName = method.getName()
        val domainClass = typeParams.psiClass
        val partTree = PartTree(methodName, domainClass)
        val holder = ProblemsHolder(manager, javaMethodPsi.containingFile, isOnTheFly)

        checkParts(holder, method, partTree)
        checkOrderBy(holder, method, partTree)
        return holder.resultsArray
    }

    private fun checkParts(holder: ProblemsHolder, uMethod: UMethod, partTree: PartTree) {
        val parts: List<Part> = partTree.parts
        if (parts.size == 1 && StringUtil.isEmptyOrSpaces(parts[0].source)) return
        for (part in parts) {
            for (property in part.property) {
                processProperty(property, uMethod, holder, partTree, part)
            }
        }
    }

    private fun processProperty(
        property: PropertyPath,
        uMethod: UMethod,
        holder: ProblemsHolder,
        partTree: PartTree,
        part: Part
    ) {
        if (property.type != null) return
        val psiIdentifier: PsiElement = uMethod.uastAnchor?.sourcePsi ?: return
        val propertyName = property.segment
        if (StringUtil.isEmptyOrSpaces(propertyName)) {
            addEmptyPropertyProblem(holder, partTree, part, psiIdentifier)
        } else {
            holder.registerProblem(
                psiIdentifier,
                TextRange(part.offset, part.endOffset),
                message("esprito.spring.data.inspection.method.name.unknown.property", propertyName)
            )
        }
    }

    private fun addEmptyPropertyProblem(
        holder: ProblemsHolder,
        partTree: PartTree,
        part: Part,
        psiIdentifier: PsiElement
    ) {
        val methodName = partTree.source
        if (methodName == partTree.subject.expression) return
        val range = TextRange.create(part.offset, part.endOffset)
        if (range.startOffset == range.endOffset) {
            val msg: String = getEmptyPropertyMessage(methodName, range)
            holder.registerProblem(
                psiIdentifier,
                message("esprito.spring.data.inspection.method.name.empty.property", msg)
            )
        } else {
            holder.registerProblem(
                psiIdentifier, range,
                message("esprito.spring.data.inspection.method.name.empty.property", "")
            )
        }
    }

    private fun checkOrderBy(holder: ProblemsHolder, uMethod: UMethod, partTree: PartTree) {
        val orderBySource = partTree.orderBySource ?: return
        val identifier: PsiElement = uMethod.uastAnchor?.sourcePsi ?: return
        for (order in orderBySource.orders) {
            for (property in order.propertyPath) {
                if (property.type != null) continue
                val propertyName = property.segment
                if (StringUtil.isEmptyOrSpaces(propertyName)) {
                    val range = TextRange.create(order.offset, order.endOffset)
                    val msg = getEmptyPropertyMessage(uMethod.name, range)
                    holder.registerProblem(
                        identifier,
                        message("esprito.spring.data.inspection.method.name.empty.property", msg)
                    )
                } else {
                    holder.registerProblem(
                        identifier,
                        TextRange.create(order.offset, order.endOffset),
                        message("esprito.spring.data.inspection.method.name.unknown.property", propertyName)
                    )
                }
            }
        }
    }

    private fun getEmptyPropertyMessage(methodName: String, range: TextRange): String {
        return methodName.substring(0, range.startOffset) + "<MISSING_PROPERTY>" + methodName.substring(range.endOffset)
    }

    private fun getPredefinedSpringDataMethods(module: Module?): Set<String> {
        module ?: return emptySet()
        val crudMethods = LibraryClassCache.searchForLibraryClass(module, SpringDataClasses.REPOSITORY_CRUD)
            ?.allMethods?.map { it.name } ?: emptyList()
        val jpaMethods = LibraryClassCache.searchForLibraryClass(module, SpringDataClasses.REPOSITORY_JPA)
            ?.allMethods?.map { it.name } ?: emptyList()
        return (crudMethods + jpaMethods).toSet()
    }
}