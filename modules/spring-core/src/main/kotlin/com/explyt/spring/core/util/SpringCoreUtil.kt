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

package com.explyt.spring.core.util

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.JavaCoreClasses
import com.explyt.spring.core.JavaEeClasses
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.SpringProperties.ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME
import com.explyt.spring.core.externalsystem.setting.NativeProjectSettings
import com.explyt.spring.core.externalsystem.utils.Constants
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.language.injection.ConfigurationPropertiesInjector
import com.explyt.spring.core.properties.SpringPropertySourceSearch
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchUtils
import com.explyt.spring.core.settings.SpringPropertyFolderState
import com.explyt.util.ExplytAnnotationUtil
import com.explyt.util.ExplytAnnotationUtil.getStringMemberValues
import com.explyt.util.ExplytAnnotationUtil.getStringValue
import com.explyt.util.ExplytPsiUtil.allSupers
import com.explyt.util.ExplytPsiUtil.deepPsiClassType
import com.explyt.util.ExplytPsiUtil.findChildrenOfType
import com.explyt.util.ExplytPsiUtil.getMetaAnnotation
import com.explyt.util.ExplytPsiUtil.isAbstract
import com.explyt.util.ExplytPsiUtil.isCollection
import com.explyt.util.ExplytPsiUtil.isEqualOrInheritor
import com.explyt.util.ExplytPsiUtil.isInterface
import com.explyt.util.ExplytPsiUtil.isMap
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedByOrSelf
import com.explyt.util.ExplytPsiUtil.isNonPrivate
import com.explyt.util.ExplytPsiUtil.isObject
import com.explyt.util.ExplytPsiUtil.isObjectProvider
import com.explyt.util.ExplytPsiUtil.isOptional
import com.explyt.util.ExplytPsiUtil.isString
import com.explyt.util.ExplytPsiUtil.psiClassType
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.explyt.util.ExplytPsiUtil.returnPsiClass
import com.explyt.util.ExplytPsiUtil.returnPsiType
import com.explyt.util.ModuleUtil
import com.explyt.util.SpringBaseClasses.CORE_ENVIRONMENT
import com.explyt.util.runReadNonBlocking
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.java.library.JavaLibraryUtil
import com.intellij.json.psi.JsonFile
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.impl.LibraryScopeCache
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.FileContextUtil
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.xdebugger.XDebuggerManager
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.toUElement
import java.util.*

object SpringCoreUtil {

    fun isConfigurationPropertyFile(psiFile: PsiFile): Boolean {
        if (SpringPropertyFolderState.isUserPropertyFolder(psiFile)) return true
        val module = ModuleUtilCore.findModuleForPsiElement(psiFile) ?: return false
        if (!isSpringProject(module)) {
            return false
        }

        if (psiFile is PropertiesFile) {
            val contextElement = FileContextUtil.getFileContext(psiFile)
            val uElement = contextElement?.toUElement()
            if (uElement != null) {
                return ConfigurationPropertiesInjector.isValidPlace(uElement)
            }
        }


        val fileExtension = FileUtilRt.getExtension(psiFile.name)
        if (!arrayOf("properties", "yaml", "yml").contains(fileExtension)) {
            return false
        }

        val fileName = FileUtilRt.getNameWithoutExtension(psiFile.name)
        if (fileName == "application"
            || fileName.startsWith("application-")
            || "config" == psiFile.parent?.name
        ) {
            return true
        }

        return runReadNonBlocking {
            val propertySourceFilePaths = SpringPropertySourceSearch.getInstance(psiFile.project)
                .findPropertySourceFilePaths(psiFile)
            if (propertySourceFilePaths.isEmpty()) {
                return@runReadNonBlocking false
            }

            val propertiesVf = psiFile.virtualFile ?: return@runReadNonBlocking false
            val sourceRootVf = ModuleUtil.getSourceRootFile(psiFile) ?: return@runReadNonBlocking false

            return@runReadNonBlocking propertySourceFilePaths.any { propertyFilePath ->
                val purePath = propertyFilePath.substringAfter(SpringProperties.PREFIX_CLASSPATH_STAR)
                    .substringAfter(SpringProperties.PREFIX_CLASSPATH)
                    .substringAfter(SpringProperties.PREFIX_FILE)

                if (purePath.substringAfterLast("/") != psiFile.name) {
                    return@any false
                }

                val targetFile = VfsUtil.findRelativeFile(purePath, sourceRootVf)
                return@any targetFile == propertiesVf
            }
        }
    }

    fun isAdditionalConfigFile(psiFile: PsiFile): Boolean {
        return psiFile is JsonFile
                && psiFile.name == ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME
    }

    private fun isSpringProject(module: Module): Boolean {
        return LibraryClassCache.searchForLibraryClass(module, SpringCoreClasses.COMPONENT) != null
    }

    fun isSpringModule(module: Module): Boolean {
        return JavaPsiFacade.getInstance(module.project)
            .findClass(SpringCoreClasses.COMPONENT, module.moduleWithLibrariesScope) != null
    }

    fun isSpringProject(project: Project): Boolean {
        return LibraryClassCache.searchForLibraryClass(project, CORE_ENVIRONMENT) != null
    }

    fun isSpringBootProject(project: Project): Boolean {
        return LibraryClassCache.searchForLibraryClass(
            project,
            SpringCoreClasses.SPRING_BOOT_APPLICATION
        ) != null
    }

    fun isSpringBootProject(module: Module): Boolean {
        return JavaPsiFacade.getInstance(module.project)
            .findClass(SpringCoreClasses.SPRING_BOOT_APPLICATION, module.moduleWithLibrariesScope) != null
    }

    /**
     * Returns whether the given PsiClass (or its inheritors) could possibly be mapped as Spring Bean.
     *
     * @param psiClass PsiClass to check.
     * @return `true` if yes.
     */
    fun isSpringBeanCandidateClass(psiClass: PsiClass): Boolean {
        return psiClass.isValid
                && psiClass !is PsiTypeParameter
                && psiClass.isNonPrivate
                && !psiClass.isAnnotationType
                && psiClass.qualifiedName != null
                && !PsiUtil.isLocalOrAnonymousClass(psiClass)
    }

    fun isComponentCandidate(psiElement: PsiElement?): Boolean {
        psiElement ?: return false
        return if (psiElement is PsiClass) {
            isComponentCandidateForClass(psiElement)
        } else {
            val uClass = psiElement.toUElement()?.getContainingUClass()
            isComponentCandidateForClass(uClass?.javaPsi)
        }
    }

    private fun isComponentCandidateForClass(psiClass: PsiClass?): Boolean {
        psiClass ?: return false

        if (psiClass.hasComponentAnnotation()) return true
        if (psiClass.hasTestComponentAnnotation()) return true
        if (psiClass.isMetaAnnotatedBy(SpringCoreClasses.CONFIGURATION_PROPERTIES)) return true
        if (!psiClass.isAbstract) return false
        if (psiClass.isInterface) return false

        val moduleScope = ModuleUtilCore.findModuleForPsiElement(psiClass)?.moduleWithDependenciesScope ?: return false
        return ClassInheritorsSearch.search(psiClass, moduleScope, true).any { it?.hasComponentAnnotation() == true }
    }

    private fun PsiClass.hasComponentAnnotation(): Boolean {
        return isMetaAnnotatedBy(SpringCoreClasses.COMPONENT) || isMetaAnnotatedBy(SpringCoreClasses.BOOTSTRAP_WITH)
    }

    private fun PsiClass.hasTestComponentAnnotation(): Boolean {
        val virtualFile = this.containingFile?.virtualFile ?: return false
        val isInTestSources = ProjectRootManager.getInstance(this.project).fileIndex.isInTestSourceContent(virtualFile)
        return isInTestSources && this.superClass?.hasComponentAnnotation() == true
    }

    fun PsiClass.getBeanName(): String? =
        name?.replaceFirstChar { it.lowercase(Locale.getDefault()) }

    fun PsiClass.resolveBeanName(module: Module): String {
        return resolveBeanNameByPsiAnnotations(SpringSearchUtils.getComponentClassAnnotations(module))
    }

    private fun PsiClass.resolveBeanNameByPsiAnnotations(annotationPsiClasses: Collection<PsiClass>): String {
        val annotationNames = annotationPsiClasses.mapNotNull { it.qualifiedName }
        return this.resolveBeanNameByAnnotations(annotationNames)
            ?: getBeanName()
            ?: throw IllegalArgumentException("Illegal bean $this")
    }

    val PsiMethod.resolveBeanName
        get() = this.resolveBeanNameByAnnotation()
            ?: setOf(name)

    fun PsiMember.resolveBeanName(module: Module): Set<String> {
        return when (this) {
            is PsiMethod -> this.resolveBeanName
            is PsiClass -> setOf(this.resolveBeanName(module))
            else -> emptySet()
        }
    }

    val PsiMember.targetClass
        get() = when (this) {
            is PsiMethod -> returnPsiClass
            is PsiClass -> this
            else -> null
        }


    val PsiVariable.resolveBeanName: String?
        get() = this.resolveBeanNameByQualifier()
            ?: name

    val PsiType.resolveBeanPsiClass: PsiClass?
        get() {
            return beanPsiType?.resolvedPsiClass
        }

    val PsiType.beanPsiType: PsiType?
        get() {
            if (this is PsiArrayType) {
                return deepPsiClassType
            }
            if (this !is PsiClassType) {
                return null
            }
            if (isCollection && isInterface) {
                // Collection<Bean>
                return parameters.firstOrNull() ?: this
            }
            if (isOptional) {
                // Optional<Bean>
                val firstParam = parameters.firstOrNull() ?: return this
                if (firstParam.isOptional) {
                    return firstParam
                }
                return firstParam.beanPsiType
            }
            if (isObjectProvider) {
                // ObjectProvider<Bean>
                return parameters.firstOrNull()?.beanPsiType ?: return this
            }
            if (isMapWithStringKey()) {
                // Map<String, Bean>
                return parameters[1]
            }
            return this
        }

    val PsiType.beanPsiTypeKotlin: PsiType?
        get() {
            if (isCollection && isInterface && this is PsiClassType) {
                val parameterType = parameters.firstOrNull()
                if (parameterType != null && parameterType is PsiWildcardType) {
                    if (parameterType.isExtends) {
                        val extendsBound = parameterType.extendsBound
                        if (extendsBound.isMap
                            || extendsBound.isCollection
                            || !extendsBound.isObject
                        ) {
                            return extendsBound
                        }
                    }
                    if (parameterType.isSuper) {
                        val superBound = parameterType.superBound
                        if (superBound.isMap
                            || superBound.isCollection
                            || !superBound.isObject
                        ) {
                            return superBound
                        }
                    }
                    return this
                }
                return parameters.firstOrNull() ?: this
            }
            if (isMap) {
                return if (isMapWithStringKey(KotlinLanguage.INSTANCE) && this is PsiClassType)
                    parameters[1]
                else this
            }
            return this.beanPsiType
        }

    fun PsiType.isMapWithStringKey(language: Language = JavaLanguage.INSTANCE): Boolean {
        return isMap
                && isInterface
                && this is PsiClassType
                && parameterCount == 2
                && parameters[0].isString
                && (language == JavaLanguage.INSTANCE
                || (language == KotlinLanguage.INSTANCE && !parameters[1].isObject))
    }

    fun PsiClass?.canResolveBeanClass(targetClasses: Set<PsiClass>): Boolean =
        this != null && targetClasses.any { it == this }

    fun PsiType.canResolveBeanClass(
        targetClasses: Set<PsiClass>,
        language: Language,
        targetClass: PsiClass? = null
    ): Boolean {
        val psiType = if (language == KotlinLanguage.INSTANCE) beanPsiTypeKotlin else beanPsiType
        return when (psiType) {
            is PsiClassType -> psiType.resolvedPsiClass.canResolveBeanClass(targetClasses)
            is PsiWildcardType -> {
                if (!psiType.isBounded && !psiType.extendsBound.isObject) {
                    return true
                }
                if (psiType.isSuper && targetClass != null) {
                    return psiType.superBound.resolvedPsiClass?.allSupers()?.any { it == targetClass } == true
                }
                targetClasses.any { it.matchesWildcardType(psiType) }
            }

            else -> false
        }
    }

    fun PsiType.isBounded(): Boolean {
        if (this is PsiWildcardType) {
            return this.isBounded
        }
        return true
    }

    private fun PsiType.supersWildcard(value: PsiType): Boolean {
        if (this is PsiWildcardType && this.isSuper) {
            return this.superBound == value || this.superBound in value.superTypes
        }
        return false
    }

    private fun PsiType.extendsWildcard(value: PsiType): Boolean {
        if (this is PsiWildcardType && this.isExtends) {
            return this.extendsBound == value || this.extendsBound in value.superTypes
        }
        return false
    }

    fun PsiType.possibleMultipleBean(): Boolean {
        if (this is PsiArrayType) {
            return true
        }
        if (this !is PsiClassType) {
            return false
        }
        if (isCollection && isInterface && parameterCount == 1 && parameters[0].isBounded()) {
            return true
        }
        if (isMap && isInterface && parameterCount == 2 && parameters[0].isString && parameters[1].isBounded()) {
            return true
        }
        return false
    }

    fun PsiType.getArrayType(): PsiArrayType? {
        var arrayPsiType: PsiArrayType? = null
        if (this is PsiClassType) {
            arrayPsiType = this.beanPsiType as? PsiArrayType
        } else if (this is PsiArrayType) {
            arrayPsiType = this
        }
        return arrayPsiType
    }

    private fun PsiModifierListOwner.resolveBeanNameByAnnotations(annotationNames: Collection<String>): String? {
        return getMetaAnnotation(annotationNames)?.getStringValue()
    }

    fun PsiModifierListOwner.resolveBeanNameByAnnotation(): Set<String>? {
        return getMetaAnnotation(SpringCoreClasses.BEAN)?.getStringMemberValues()?.ifEmpty { null }?.toSet()
    }

    fun PsiAnnotation.resolveBeanName(): String? {
        if (isMetaAnnotatedByOrSelf(SpringCoreClasses.QUALIFIER)) {
            if (resolveAnnotationType()?.methods?.size == 1) {
                return getStringValue()
            }
            return null
        }
        if (isMetaAnnotatedByOrSelf(JavaEeClasses.QUALIFIER.allFqns)) {
            return getStringValue()
        }
        return null
    }

    fun PsiModifierListOwner.resolveBeanNameByQualifier(): String? {
        return getMetaAnnotation(SpringCoreClasses.QUALIFIERS)?.resolveBeanName()
    }

    val PsiModifierListOwner.resolvePsiClass: PsiClass?
        get() {
            if (this is PsiClass) return this
            if (this is PsiMethod) return this.returnPsiClass
            return null
        }

    fun PsiModifierListOwner.getQualifierAnnotation(): PsiAnnotation? {
        return this.getMetaAnnotation(SpringCoreClasses.QUALIFIERS)
    }

    fun PsiType.isEqualOrInheritorBeanType(beanPsiType: PsiType): Boolean {
        if (this == beanPsiType) {
            return true
        }
        if (this is PsiArrayType && this.isEqualOrInheritor(beanPsiType) && beanPsiType.isObject) {
            return true
        }
        if (this !is PsiClassType) {
            return false
        }
        if (beanPsiType is PsiClassType) {
            if (beanPsiType.isCollection) {
                val parameter = beanPsiType.parameters.firstOrNull()
                if (parameter != null && parameter is PsiClassType && this.isObject) {
                    return parameter.isEqualOrInheritor(this)
                }
            }
            return this.isEqualOrInheritor(beanPsiType)
                    && (beanPsiType.parameters.isEmpty() || this.equalParamsWithBound(beanPsiType)
                    || this.isAllParametersAssignable(beanPsiType))
        }
        if (beanPsiType is PsiWildcardType) {
            return this.matchesWildcardType(beanPsiType)
        }
        return false
    }

    fun PsiClassType.equalParamsWithBound(otherPsiType: PsiClassType): Boolean {
        if (parameters.size != otherPsiType.parameters.size) {
            return false
        }
        return this.parameters.withIndex().all { (index, value) ->
            otherPsiType.parameters[index].let {
                it == value
                        || it.extendsWildcard(value)
                        || value.extendsWildcard(it)
                        || it.supersWildcard(value)
                        || !it.isBounded()
            }
        }
    }

    fun PsiClass.matchesWildcardType(beanPsiType: PsiWildcardType): Boolean {
        if (beanPsiType.isExtends) {
            val extendsBeanPsiClass = beanPsiType.extendsBound.resolvedPsiClass
            if (extendsBeanPsiClass != null) {
                return this.isEqualOrInheritor(extendsBeanPsiClass)
            }
        }
        return isWildcardTypeSuper(this, beanPsiType)
    }

    fun PsiClassType.matchesWildcardType(beanPsiType: PsiWildcardType): Boolean {
        val resolvedPsiClass = this.resolvedPsiClass ?: return false
        if (beanPsiType.isExtends) {
            val extendsBeanType = beanPsiType.extendsBound
            val extendsBeanPsiClass = extendsBeanType.resolvedPsiClass
            if (extendsBeanPsiClass != null) {
                return if (extendsBeanType is PsiClassType) {
                    resolvedPsiClass.isEqualOrInheritor(extendsBeanPsiClass)
                            && (extendsBeanType.parameters.isEmpty() || this.equalParamsWithBound(extendsBeanType))

                } else resolvedPsiClass.isEqualOrInheritor(extendsBeanPsiClass)
            }
        }
        return isWildcardTypeSuper(resolvedPsiClass, beanPsiType)
    }

    private fun isWildcardTypeSuper(psiClass: PsiClass, beanPsiType: PsiWildcardType): Boolean {
        if (beanPsiType.isSuper) {
            val superBeanPsiClass = beanPsiType.superBound.resolvedPsiClass
            if (superBeanPsiClass != null) {
                return superBeanPsiClass == psiClass
            }
        }
        return false
    }

    fun Collection<PsiMethod>.filterByInheritedTypes(
        sourcePsiType: PsiType,
        beanPsiType: PsiType?
    ): Sequence<PsiMethod> {
        var sequence = this.asSequence()
        if (this.isEmpty()) {
            return sequence
        }
        val beanPsiClass = beanPsiType?.resolvedPsiClass
        if (beanPsiClass != null && beanPsiType != sourcePsiType) {
            val possibleCustomInheritors = SpringSearchUtils.searchClassInheritors(beanPsiClass)
            if (possibleCustomInheritors.isNotEmpty()) {
                sequence = sequence.filter {
                    it.returnPsiClass?.let {
                        it in possibleCustomInheritors || possibleCustomInheritors.any { inheritorClass ->
                            inheritorClass.isEqualOrInheritor(
                                it
                            )
                        }
                    } == true
                }
            }
        }

        return sequence.filter { psiMethod ->//
            val psiMethodReturnType = psiMethod.returnType
            psiMethod.findChildrenOfType<PsiReturnStatement>().any {
                it.returnValue?.type?.let {
                    it != psiMethodReturnType && (
                            it.isEqualOrInheritorBeanType(sourcePsiType)
                                    || beanPsiType != null && beanPsiType != sourcePsiType && it.isEqualOrInheritorBeanType(
                                beanPsiType
                            )
                            )
                } == true
            }
        }
    }

    @Suppress("UnstableApiUsage")
    fun hasBootLibrary(module: Module): Boolean {
        return JavaLibraryUtil.hasLibraryJar(module, SPRING_BOOT_MAVEN)
    }

    fun String.removeDummyIdentifier(): String {
        return this.replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "")
            .replace("$,", "")
            .replace(" ", "")
    }

    fun getClassMethodsFromLibraries(fullyQualifiedName: String, module: Module): Array<PsiMethod>? {
        val shortName = fullyQualifiedName.split('.').last()

        return PsiShortNamesCache.getInstance(module.project)
            .getClassesByName(
                shortName,
                LibraryScopeCache.getInstance(module.project).librariesOnlyScope
            )
            .firstOrNull { it.qualifiedName == fullyQualifiedName }
            ?.methods
    }


    fun PsiParameter.isCandidate(
        targetType: PsiType?,
        targetClass: PsiClass,
        targetClasses: Set<PsiClass>
    ): Boolean {
        return targetType == this.type
                || (targetType == null && targetClass.qualifiedName == this.type.resolvedPsiClass?.qualifiedName)
                || this.type.canResolveBeanClass(targetClasses, this.language)
    }

    fun PsiField.isCandidate(
        targetType: PsiType?,
        targetClasses: Set<PsiClass>,
        targetClass: PsiClass
    ): Boolean {
        if (targetType == this.type) return true
        if (targetType == null && targetClass.qualifiedName == this.type.resolvedPsiClass?.qualifiedName) return true
        if (targetType != null && targetType.isEqualOrInheritorBeanType(this.type)) return true
        if (targetType != null && this.type.isAssignableFrom(targetType)) return true

        if (targetType is PsiArrayType) {
            return if (this.language == KotlinLanguage.INSTANCE) getPsiType(this) == targetType
            else getPsiType(this)?.isAssignableFrom(targetType) == true
        }

        val isResolved = this.type.canResolveBeanClass(targetClasses, this.language, targetClass)
        if (!isResolved) return false
        if (targetType == null) return true

        if (targetType !is PsiClassType) return true
        val psiClassType = getPsiType(this)
        return if (targetType.parameters.isNotEmpty()) {
            psiClassType?.isEqualOrInheritorBeanType(targetType) == true
        } else true
    }

    fun getPsiType(field: PsiField): PsiType? {
        val type = field.type.beanPsiType
        if (type is PsiClassType) {
            return type.psiClassType
        }
        if (type is PsiArrayType) {
            return type
        }
        if (field.language == KotlinLanguage.INSTANCE && type is PsiWildcardType) {
            if (type.isExtends) return type.extendsBound
            if (type.isSuper) return type.superBound
        }
        return null
    }

    fun Collection<PsiMethod>.filterByExactMatch(sourcePsiType: PsiType): Sequence<PsiMethod> {
        val isSourcePsiTypeHasParameters = sourcePsiType.psiClassType?.let { it.parameterCount > 0 } == true
        val isSourcePsiTypeHasSingleUnboundedWildcardType = sourcePsiType.psiClassType?.let {
            it.parameterCount == 1 && !it.parameters[0].isBounded()
        } == true

        val resolvedSourcePsiClass = sourcePsiType.resolvedPsiClass
        return this.asSequence().filter {
            it.returnType?.isEqualOrInheritorBeanType(sourcePsiType) ?: false ||
                    it.returnType == sourcePsiType
                    || it.returnPsiType?.isEqualOrInheritorBeanType(sourcePsiType) == true
                    || (!isSourcePsiTypeHasParameters || isSourcePsiTypeHasSingleUnboundedWildcardType)
                    && resolvedSourcePsiClass != null && it.returnPsiClass == resolvedSourcePsiClass
        }
    }


    fun Collection<PsiBean>.filterByExactMatchRegistrar(sourcePsiType: PsiType): Sequence<PsiMethod> {
        val isSourcePsiTypeHasParameters = sourcePsiType.psiClassType?.let { it.parameterCount > 0 } == true
        val isSourcePsiTypeHasSingleUnboundedWildcardType = sourcePsiType.psiClassType?.let {
            it.parameterCount == 1 && !it.parameters[0].isBounded()
        } == true

        val resolvedSourcePsiClass = sourcePsiType.resolvedPsiClass
        return this.asSequence().filter {
            InheritanceUtil.isInheritorOrSelf(it.psiClass, resolvedSourcePsiClass, false)
                    || (!isSourcePsiTypeHasParameters || isSourcePsiTypeHasSingleUnboundedWildcardType)
                    && resolvedSourcePsiClass != null && it.psiClass == resolvedSourcePsiClass
        }.mapNotNull { it.psiMember as? PsiMethod }
    }


    fun PsiType.isExactMatch(sourcePsiType: PsiType): Boolean {
        val isSourcePsiTypeHasParameters = sourcePsiType.psiClassType?.let { it.parameterCount > 0 } == true
        val isSourcePsiTypeHasSingleUnboundedWildcardType = sourcePsiType.psiClassType?.let {
            it.parameterCount == 1 && !it.parameters[0].isBounded()
        } == true

        val resolvedSourcePsiClass = sourcePsiType.resolvedPsiClass
        return this.isEqualOrInheritorBeanType(sourcePsiType) ||
                this == sourcePsiType
                || (!isSourcePsiTypeHasParameters || isSourcePsiTypeHasSingleUnboundedWildcardType)
                && resolvedSourcePsiClass != null && this.resolvedPsiClass == resolvedSourcePsiClass
    }

    fun Collection<PsiMethod>.filterByBeanPsiType(beanPsiType: PsiType): Sequence<PsiMethod> {
        val inheritedPsiMethods = this.asSequence().filter {
            it.returnPsiType?.isEqualOrInheritorBeanType(beanPsiType) == true
        }
        // This function added a candidate to the beans, where returned other type.
        // Example: @Bean E dBean() { return new D(); }
        // val filterByInheritedTypes = this.filterByInheritedTypes(sourcePsiType, beanPsiType)
        return inheritedPsiMethods // + filterByInheritedTypes
    }

    fun Collection<PsiBean>.filterByBeanPsiTypeRegistrar(beanPsiType: PsiType): Sequence<PsiMethod> {
        val qualifiedName = beanPsiType.resolveBeanPsiClass?.qualifiedName ?: return emptySequence()
        return this.asSequence().filter {
            InheritanceUtil.isInheritor(it.psiClass, qualifiedName)
        }.mapNotNull { it.psiMember as? PsiMethod }
    }

    fun PsiType.isEqualOrInheritorType(beanPsiType: PsiType): Boolean {
        return this.isEqualOrInheritorBeanType(beanPsiType)
    }


    fun PsiMember.filterByQualifier(
        module: Module, qualifier: PsiAnnotation?, beanNameFromQualifier: String?
    ): Boolean {
        return qualifier == null
                || beanNameFromQualifier != null && beanNameFromQualifier in this.resolveBeanName(module)
                || ExplytAnnotationUtil.equal(qualifier, this.getAnnotation(qualifier.qualifiedName!!))
    }

    fun PsiBean.filterByQualifier(qualifier: PsiAnnotation?, beanNameFromQualifier: String?): Boolean {
        return qualifier == null
                || beanNameFromQualifier != null && beanNameFromQualifier == this.name
                || ExplytAnnotationUtil.equal(qualifier, this.psiMember.getAnnotation(qualifier.qualifiedName!!))
    }

    fun PsiClass.isMimeTypeClass(): Boolean {
        return ((this.qualifiedName == SpringCoreClasses.MIME_TYPE)
                || (this.superClass?.qualifiedName == SpringCoreClasses.MIME_TYPE)
                || (this.interfaces.any { it.qualifiedName == SpringCoreClasses.MIME_TYPE }))
    }

    fun PsiClass.isCharsetTypeClass(): Boolean {
        return ((this.qualifiedName == JavaCoreClasses.CHARSET)
                || (this.superClass?.qualifiedName == JavaCoreClasses.CHARSET)
                || (this.interfaces.any { it.qualifiedName == JavaCoreClasses.CHARSET }))
    }

    fun PsiClassType.isAllParametersAssignable(otherPsiType: PsiClassType): Boolean {
        if (otherPsiType.isCollection || otherPsiType.isMap) return false
        if (this.parameterCount != otherPsiType.parameterCount) return false
        this.parameters.forEachIndexed { idx, it ->
            if (!it.isAssignableFrom(otherPsiType.parameters[idx])) return false
        }
        return true
    }

    fun isExplytDebug(project: Project): Boolean {
        if (!SpringToolRunConfigurationsSettingsState.getInstance().isDebugMode) return false
        val debugSession = XDebuggerManager.getInstance(project).currentSession
        if (debugSession != null) {
            val debugProjectSettings = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
                .getLinkedProjectSettings(Constants.DEBUG_SESSION_NAME) as? NativeProjectSettings
            return debugProjectSettings != null
                    && debugProjectSettings.runConfigurationId != null
                    && debugProjectSettings.runConfigurationId == debugSession.sessionName
        }
        return false
    }

    const val SPRING_BOOT_MAVEN = "org.springframework.boot:spring-boot"
    const val SPRING_BOOT_ACTUATOR_MAVEN = "org.springframework.boot:spring-boot-actuator"
    const val BASE_PACKAGES = "basePackages"
    const val SCAN_BASE_PACKAGES = "scanBasePackages"

}
