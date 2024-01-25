package com.esprito.spring.data.completion

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.esprito.spring.core.util.PsiAnnotationUtils
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.esprito.spring.data.SpringDataClasses
import com.esprito.spring.data.util.SpringDataUtil
import com.esprito.util.EspritoPsiUtil.returnPsiType
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.util.ObjectUtils
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.*
import kotlin.math.max

class SpringDataBaseCompletionProvider : CompletionProvider<CompletionParameters>() {
    private val existPattern = listOf("exists")
    private val countPattern = listOf("count")
    private val removePattern = listOf("delete", "remove")
    private val queryPattern = listOf("find", "read", "get", "query", "stream", "search")

    private val distinctAllSuffix = listOf("Distinct", "All")
    private val firstTopSuffix = listOf("First", "Top", "DistinctFirst", "DistinctTop")
    private val allOperators = listOf(
        "Distinct", "And", "Or", "Is", "Equals",
        "Between", "LessThan", "LessThanEqual", "GreaterThan", "GreaterThanEqual", "After",
        "Before", "IsNull", "Null", "IsNotNull", "NotNull", "Like",
        "NotLike", "StartingWith", "EndingWith", "Containing", "OrderBy", "Not",
        "In", "NotIn", "True", "False", "IgnoreCase", "Desc", "Asc"
    )

    override fun addCompletions(
        parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet
    ) {
        val repositoryTypes = getRepositoryTypes(parameters) ?: return
        val positionContext = getCurrentPositionContext(parameters) ?: return
        result.addAllElements(getCompletionVariants(repositoryTypes, positionContext, result.prefixMatcher))
        result.stopHere()
    }

    private fun getCompletionVariants(
        typesPair: Pair<PsiClass, PsiType?>, positionContext: PositionContext, matcher: PrefixMatcher
    ): Set<LookupElement> {
        val methodName = positionContext.methodName
        val methodPrefix = matcher.prefix
        val domainClass: PsiClass = typesPair.first
        val offset = methodPrefix.length

        val subjectVariants = getSubjectVariants(domainClass, positionContext)
        val domainProperties = SpringDataUtil.getProperties(domainClass).map { StringUtil.capitalize(it) } +
                (domainClass.name?.let { listOf(it, StringUtil.pluralize(it)) } ?: listOf())

        //start of input: <caret>/fin<caret>/r<caret>/remov<caret> and etc
        if (subjectVariants.contains(methodPrefix)) {
            return getCompletionVariants(domainProperties.toSet(), methodPrefix, "")
        } else if (StringUtil.isEmptyOrSpaces(methodPrefix) || subjectVariants.any { it.startsWith(methodPrefix) }) {
            return getCompletionVariants(subjectVariants, "", "")
        }

        val endWithProperty = domainProperties.find { methodPrefix.endsWith(it) }
        val suffix = getSuffix(methodName, offset)
        if (endWithProperty != null) {
            //case when caret after property: findById<Caret>/findByName<caret>OrAddress
            return getCompletionVariants(allOperators.toSet(), methodPrefix, suffix)
        } else {
            val prefix = getPrefix(methodName, offset)
            val lookupStrings = (domainProperties + allOperators).toSet()
            return getLookupElements(lookupStrings, prefix, suffix, methodPrefix)
        }
    }

    private fun SpringDataBaseCompletionProvider.getLookupElements(
        lookupStrings: Set<String>, prefix: String, suffix: String, methodPrefix: String
    ): Set<LookupElement> {
        if (methodPrefix == prefix) return getCompletionVariants(lookupStrings, prefix, suffix)
        return getCompletionVariants(lookupStrings, prefix, suffix) +
                getCompletionVariants(lookupStrings, methodPrefix, suffix)
    }

    private fun getRepositoryTypes(parameters: CompletionParameters): Pair<PsiClass, PsiType?>? {
        val aClass: UClass = parameters.position.findContaining(UClass::class.java) ?: return null
        return substituteRepositoryTypes(aClass.javaPsi)
    }

    private fun substituteRepositoryTypes(repositoryClass: PsiClass): Pair<PsiClass, PsiType>? {
        if (AnnotationUtil.isAnnotated(
                repositoryClass, SpringDataClasses.REPOSITORY_ANNOTATION,
                AnnotationUtil.CHECK_HIERARCHY
            )
        ) {
            return substituteForRepositoryDefinition(repositoryClass)
        }
        val psiClassType = JavaPsiFacade.getInstance(repositoryClass.project)
            .elementFactory.createType(repositoryClass)
        val psiType = PsiUtil.substituteTypeParameter(psiClassType, SpringDataClasses.SPRING_RESOURCE, 0, false)
                as? PsiClassType ?: return null

        val idPsiType =
            PsiUtil.substituteTypeParameter(psiClassType, SpringDataClasses.SPRING_RESOURCE, 1, false) ?: return null
        val psiClass = psiType.resolve() ?: return null
        return Pair(psiClass, idPsiType)
    }

    private fun substituteForRepositoryDefinition(repositoryClass: PsiClass): Pair<PsiClass, PsiType>? {
        val module = ModuleUtilCore.findModuleForPsiElement(repositoryClass) ?: return null
        val metaAnnotationsHolder = MetaAnnotationsHolder.of(module, SpringDataClasses.REPOSITORY_ANNOTATION)
        val annotation = repositoryClass.annotations.find { metaAnnotationsHolder.contains(it) } ?: return null
        val domainValues = metaAnnotationsHolder.getAnnotationMemberValues(annotation, setOf("domainClass"))
        val idValues = metaAnnotationsHolder.getAnnotationMemberValues(annotation, setOf("idClass"))
        val classDomain = PsiAnnotationUtils.getPsiTypes(domainValues).map { it.resolveBeanPsiClass }.firstOrNull()
        val typeId = PsiAnnotationUtils.getPsiTypes(idValues).firstOrNull()
        return if (classDomain != null && typeId != null) Pair(classDomain, typeId) else null
    }

    private fun getCurrentPositionContext(parameters: CompletionParameters): PositionContext? {
        var declaration = parameters.position.findContaining(UDeclaration::class.java)
        if (declaration is UParameter) {
            declaration = ObjectUtils.tryCast(declaration.uastParent, UDeclaration::class.java)
        }
        if (declaration == null || declaration.uastParent !is UClass) return null
        val sourceElement = declaration.sourcePsi ?: return null

        val originalElement = getOriginalElement(sourceElement, PsiElement::class.java)
        val psiType = psiTypeElement(originalElement)
        val originalUDeclaration = originalElement.findContaining(UDeclaration::class.java)
        return (originalUDeclaration as? UAnchorOwner)?.uastAnchor?.name?.let { PositionContext(it, psiType) }
    }

    private fun psiTypeElement(originalElement: PsiElement?): PsiType? {
        if (originalElement is PsiTypeElement) {
            return originalElement.type
        }
        return (originalElement.toUElement() as? UField)?.returnPsiType
    }

    private fun getSubjectVariants(
        domainClassName: PsiClass, positionContext: PositionContext? = null
    ): Set<String> {
        val strings = HashSet<String>()
        if (positionContext?.psiType?.isAssignableFrom(PsiTypes.voidType()) == true) {
            removePattern.forEach { addSubjectSimpleTails(strings, it, domainClassName) }
        } else if (positionContext?.psiType?.isAssignableFrom(PsiTypes.booleanType()) == true) {
            existPattern.forEach { addSubjectSimpleTails(strings, it, domainClassName) }
        } else if (isNumberType(positionContext)) {
            countPattern.forEach { addSubjectSimpleTails(strings, it, domainClassName) }
            removePattern.forEach { addSubjectSimpleTails(strings, it, domainClassName) }
        } else if (positionContext?.psiType == null) {
            addQueryPattern(strings, domainClassName)
            removePattern.forEach { addSubjectSimpleTails(strings, it, domainClassName) }
            existPattern.forEach { addSubjectSimpleTails(strings, it, domainClassName) }
            countPattern.forEach { addSubjectSimpleTails(strings, it, domainClassName) }
            removePattern.forEach { addSubjectSimpleTails(strings, it, domainClassName) }
        } else {
            addQueryPattern(strings, domainClassName)
        }
        return strings.mapTo(mutableSetOf()) { s: String -> s + "By" }
    }

    private fun isNumberType(positionContext: PositionContext?) =
        positionContext?.psiType?.isAssignableFrom(PsiTypes.intType()) == true
                || positionContext?.psiType?.isAssignableFrom(PsiTypes.longType()) == true
                || positionContext?.psiType?.isAssignableFrom(PsiTypes.byteType()) == true

    private fun addQueryPattern(strings: HashSet<String>, domainClassName: PsiClass) {
        for (s in queryPattern) {
            addSubjectSimpleTails(strings, s, domainClassName)
            addSimpleTails(strings, s)
        }
    }

    private fun addSubjectSimpleTails(strings: MutableCollection<String>, base: String, domainClass: PsiClass) {
        addTails(strings, base, distinctAllSuffix)
        val domainClassName = domainClass.name
        if (domainClassName != null) {
            addTails(strings, base, listOf(domainClassName, StringUtil.pluralize(domainClassName)))
        }
    }

    private fun addSimpleTails(strings: MutableCollection<String>, s: String) {
        addTails(strings, s, firstTopSuffix)
    }

    private fun addTails(strings: MutableCollection<String>, base: String, tails: Collection<String>) {
        strings.add(base)
        for (tail in tails) {
            strings.add(base + tail)
        }
    }

    private fun getSuffix(methodName: String, offset: Int): String {
        val charArray = methodName.toCharArray()
        for (i in charArray.indices) {
            if (i >= offset && Character.isUpperCase(charArray[i])) {
                return methodName.substring(i)
            }
        }
        return ""
    }

    private fun getPrefix(methodName: String, offset: Int): String {
        val charArray = methodName.toCharArray()
        var maxUpperCasePosition = 0
        for (i in charArray.indices) {
            if (i <= offset && Character.isUpperCase(charArray[i])) {
                maxUpperCasePosition = max(i, maxUpperCasePosition)
            }
        }
        return if (maxUpperCasePosition > 0) methodName.substring(0, maxUpperCasePosition) else ""
    }

    private fun getCompletionVariants(
        strings: Set<String>, prefixExpression: String, suffixExpression: String?
    ): Set<LookupElement> {
        val set = HashSet<LookupElement>()
        for (s in strings) {
            if (StringUtil.isEmptyOrSpaces(s)) continue
            set.add(createLookupElement(prefixExpression, s, ""))
            if (!StringUtil.isEmptyOrSpaces(suffixExpression)) {
                set.add(createLookupElement(prefixExpression, s, suffixExpression!!))
            }
        }
        return set
    }

    private fun createLookupElement(
        prefixExpression: String, property: String, suffixExpression: String
    ): LookupElementBuilder {
        val lookupText = prefixExpression + property + suffixExpression
        return LookupElementBuilder.create(lookupText).withPresentableText(property).bold()
            .withTailText(if (StringUtil.isEmptyOrSpaces(suffixExpression)) "" else suffixExpression, true)
            .withIcon(SpringIcons.Spring).withInsertHandler(object : InsertHandler<LookupElement> {
                override fun handleInsert(context: InsertionContext, item: LookupElement) {
                    AutoPopupController.getInstance(context.project).scheduleAutoPopup(context.editor)
                }
            })
    }

    private fun <T : PsiElement?> getOriginalElement(psiElement: T, elementClass: Class<out T>): T? {
        if (psiElement == null) return null
        val psiFile = psiElement.containingFile
        val originalFile = psiFile.originalFile
        if (originalFile === psiFile) return psiElement
        val range = psiElement.textRange
        val element = originalFile.findElementAt(range.startOffset)
        val maxLength = range.length
        var parent = PsiTreeUtil.getParentOfType(element, elementClass, false)
        var next = parent
        while (next != null && next.textLength <= maxLength) {
            parent = next
            next = PsiTreeUtil.getParentOfType(next, elementClass, true)
        }
        return parent
    }
}

private data class PositionContext(val methodName: String, val psiType: PsiType?)