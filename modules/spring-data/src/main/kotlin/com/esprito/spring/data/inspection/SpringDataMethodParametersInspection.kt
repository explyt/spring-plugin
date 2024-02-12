package com.esprito.spring.data.inspection

import com.esprito.spring.data.SpringDataBundle.message
import com.esprito.spring.data.SpringDataClasses
import com.esprito.spring.data.SpringDataClasses.DOMAIN_PACKAGE_PREFIX
import com.esprito.spring.data.util.SpringDataRepositoryUtil
import com.esprito.util.TypeQuickFixUtil
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiType
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.getParentOfType
import org.springframework.data.repository.query.parser.Part
import org.springframework.data.repository.query.parser.PartTree
import org.springframework.data.repository.query.parser.domain.PropertyPath


class SpringDataMethodParametersInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkMethod(
        method: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val uClass = method.getParentOfType<UClass>() ?: return emptyArray()
        val typeParams = SpringDataRepositoryUtil.substituteRepositoryTypes(uClass.javaPsi) ?: return emptyArray()
        if (AnnotationUtil.isAnnotated(method.javaPsi, SpringDataClasses.QUERY, AnnotationUtil.CHECK_HIERARCHY)) {
            return emptyArray()
        }

        val methodName = method.getName()
        val domainClass = typeParams.psiClass
        val partTree = PartTree(methodName, domainClass)
        val parts = partTree.parts
        if (notCompleteMethod(parts)) return emptyArray()

        val holder = ProblemsHolder(manager, method.javaPsi.containingFile, isOnTheFly)
        validateParametersCountAndCollectionType(method, parts, holder)
        validateParametersType(method, parts, holder)
        return holder.resultsArray
    }

    private fun validateParametersCountAndCollectionType(method: UMethod, parts: List<Part>, holder: ProblemsHolder) {
        var argCount = 0
        val uParameters = method.uastParameters
        val lastIndexCount = uParameters.size - 1
        for (part in parts) {
            val numberOfArguments = part.numberOfArguments
            for (i in 0 until numberOfArguments) {
                if (uParameters.isEmpty() || lastIndexCount < argCount) {
                    parametersCountProblem(method, holder, argCount, part)
                    return
                }
                val uParameter = uParameters[argCount]
                val psiType = uParameter.typeReference?.type ?: return
                if (expectsCollection(part.type) && !parameterIsCollectionLike(psiType)) {
                    expectsCollectionProblem(holder, uParameter, part, psiType)
                    return
                }
                if (!expectsCollection(part.type) && !parameterIsScalarLike(psiType)) {
                    notExpectCollectionProblem(holder, uParameter, part, psiType)
                    return
                }
                argCount++
            }
        }
        validateRedundantParameters(argCount, method, uParameters, holder)
    }

    private fun validateRedundantParameters(
        argCount: Int,
        method: UMethod,
        uParameters: List<UParameter>,
        holder: ProblemsHolder
    ) {
        if (holder.hasResults()) return
        val sourcePsi = method.uastAnchor?.sourcePsi ?: return
        val parameterCount = uParameters
            .count { it.typeReference?.type?.canonicalText?.startsWith(DOMAIN_PACKAGE_PREFIX) == false }
        if (argCount < parameterCount) {
            holder.registerProblem(
                sourcePsi,
                message("esprito.spring.data.inspection.method.parameters.redundant", parameterCount, argCount)
            )
        }
    }

    private fun validateParametersType(method: UMethod, parts: List<Part>, holder: ProblemsHolder) {
        var argCount = 0
        val uParameters = method.uastParameters
        for (part in parts) {
            val numberOfArguments = part.numberOfArguments
            for (i in 0 until numberOfArguments) {
                if (holder.hasResults()) return

                val uParameter = uParameters[argCount]
                val actualParameterType = uParameter.typeReference?.type ?: return
                val expectedEntityType = getLastProperty(part).type ?: return

                val expectedType = TypeQuickFixUtil.getUnwrapTargetType(expectedEntityType) ?: return
                val actualType = TypeQuickFixUtil.getUnwrapTargetType(actualParameterType) ?: return

                if (!actualType.isAssignableFrom(expectedType)) {
                    expectedTypeProblem(uParameter, holder, expectedType, part.type)
                }
                argCount++
            }
        }
    }

    private fun notExpectCollectionProblem(
        holder: ProblemsHolder,
        uParameter: UParameter,
        part: Part,
        psiType: PsiType
    ) {
        val psiParameter = uParameter.sourcePsi ?: return
        val expectedEntityType = getLastProperty(part).type?.let { TypeQuickFixUtil.getUnwrapTargetType(it) }
        holder.registerProblem(
            psiParameter,
            message(
                "esprito.spring.data.inspection.method.parameters.scalar",
                part.type.name, part.property.toDotPath(), psiType.presentableText
            ),
            *TypeQuickFixUtil.getQuickFixes(uParameter, expectedEntityType)
        )
    }

    private fun expectsCollectionProblem(
        holder: ProblemsHolder,
        uParameter: UParameter,
        part: Part,
        psiType: PsiType
    ) {
        val psiParameter = uParameter.sourcePsi ?: return

        val expectedEntityType = getLastProperty(part).type?.let { TypeQuickFixUtil.getUnwrapTargetType(it) }
        val wrapToCollection = TypeQuickFixUtil.wrapToCollection(psiParameter.project, expectedEntityType)
        holder.registerProblem(
            psiParameter,
            message(
                "esprito.spring.data.inspection.method.parameters.collection",
                part.type.name, part.property.toDotPath(), psiType.presentableText
            ),
            *TypeQuickFixUtil.getQuickFixes(uParameter, wrapToCollection)
        )
    }

    private fun parametersCountProblem(
        method: UMethod,
        holder: ProblemsHolder,
        i: Int,
        part: Part
    ) {
        val psiIdentifier = method.uastAnchor?.sourcePsi ?: return
        holder.registerProblem(
            psiIdentifier,
            message(
                "esprito.spring.data.inspection.method.parameters.count",
                i + 1, i, part.type.name, part.property.toDotPath()
            )
        )
    }

    private fun notCompleteMethod(parts: MutableList<Part>): Boolean {
        for (part in parts) {
            for (property in part.property) {
                if (property.type == null) {
                    return true
                }
            }
        }
        return false
    }

    private fun getLastProperty(part: Part): PropertyPath {
        var property = part.property
        while (property.hasNext()) {
            property = property.next()
        }
        return property
    }

    private fun expectedTypeProblem(
        uParameter: UParameter,
        holder: ProblemsHolder,
        expectedEntityType: PsiType,
        type: Part.Type
    ) {
        val sourcePsi = uParameter.sourcePsi ?: return
        val project = holder.project
        val quickFixeType = if (expectsCollection(type)) {
            TypeQuickFixUtil.wrapToCollection(project, expectedEntityType)
        } else expectedEntityType

        val expectedTypeString = if (expectsCollection(type))
            "Collection<${expectedEntityType.presentableText}>" else expectedEntityType.presentableText
        holder.registerProblem(
            sourcePsi,
            message("esprito.spring.data.inspection.method.parameters.expected", expectedTypeString),
            *TypeQuickFixUtil.getQuickFixes(uParameter, quickFixeType)
        )
    }

    private fun parameterIsCollectionLike(type: PsiType): Boolean {
        return InheritanceUtil.isInheritor(type, Iterable::class.java.name) || type is PsiArrayType
    }

    private fun parameterIsScalarLike(type: PsiType): Boolean {
        return !InheritanceUtil.isInheritor(type, Iterable::class.java.name)
    }

    private fun expectsCollection(type: Part.Type): Boolean {
        return type == Part.Type.IN || type == Part.Type.NOT_IN
    }
}