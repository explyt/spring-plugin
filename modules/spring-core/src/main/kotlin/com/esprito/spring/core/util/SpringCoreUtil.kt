package com.esprito.spring.core.util

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.JavaEeClasses
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.SpringProperties.ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME
import com.esprito.spring.core.language.injection.ConfigurationPropertiesInjector
import com.esprito.spring.core.properties.SpringPropertySourceSearch
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.util.EspritoAnnotationUtil.getStringMemberValues
import com.esprito.util.EspritoAnnotationUtil.getStringValue
import com.esprito.util.EspritoPsiUtil.allSupers
import com.esprito.util.EspritoPsiUtil.deepPsiClassType
import com.esprito.util.EspritoPsiUtil.findChildrenOfType
import com.esprito.util.EspritoPsiUtil.getMetaAnnotation
import com.esprito.util.EspritoPsiUtil.isCollection
import com.esprito.util.EspritoPsiUtil.isEqualOrInheritor
import com.esprito.util.EspritoPsiUtil.isInterface
import com.esprito.util.EspritoPsiUtil.isMap
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedByOrSelf
import com.esprito.util.EspritoPsiUtil.isNonPrivate
import com.esprito.util.EspritoPsiUtil.isObject
import com.esprito.util.EspritoPsiUtil.isOptional
import com.esprito.util.EspritoPsiUtil.isString
import com.esprito.util.EspritoPsiUtil.resolvedPsiClass
import com.esprito.util.EspritoPsiUtil.returnPsiClass
import com.esprito.util.ModuleUtil
import com.esprito.util.runReadNonBlocking
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.java.library.JavaLibraryUtil
import com.intellij.json.psi.JsonFile
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.LibraryScopeCache
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.FileContextUtil
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.toUElement
import java.util.*

object SpringCoreUtil {

    fun isConfigurationPropertyFile(psiFile: PsiFile): Boolean {
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

    fun isSpringBootProject(project: Project): Boolean {
        return LibraryClassCache.searchForLibraryClass(
            project,
            SpringCoreClasses.SPRING_BOOT_APPLICATION
        ) != null
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
        return psiClass != null && (psiClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)
                || psiClass.isMetaAnnotatedBy(SpringCoreClasses.BOOTSTRAP_WITH))
    }

    fun PsiClass.getBeanName(): String? =
        name?.replaceFirstChar { it.lowercase(Locale.getDefault()) }

    fun PsiClass.resolveBeanName(module: Module): String {
        return resolveBeanNameByPsiAnnotations(
            SpringSearchService.getInstance(module.project).getComponentClassAnnotations(module)
        )
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
                    && (beanPsiType.parameters.isEmpty() || this.equalParamsWithBound(beanPsiType))
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

    fun Collection<PsiMethod>.filterByInheritedTypes(sourcePsiType: PsiType, beanPsiType: PsiType?): Sequence<PsiMethod> {
        var sequence = this.asSequence()
        if (this.isEmpty()) {
            return sequence
        }
        val beanPsiClass = beanPsiType?.resolvedPsiClass
        if (beanPsiClass != null && beanPsiType != sourcePsiType) {
            val possibleCustomInheritors = SpringSearchService.getInstance(this.first().project).searchClassInheritors(beanPsiClass)
            if (possibleCustomInheritors.isNotEmpty()) {
                sequence = sequence.filter {
                    it.returnPsiClass?.let {
                        it in possibleCustomInheritors || possibleCustomInheritors.any { inheritorClass -> inheritorClass.isEqualOrInheritor(it) }
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
                                || beanPsiType != null && beanPsiType != sourcePsiType && it.isEqualOrInheritorBeanType(beanPsiType)
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

    const val SPRING_BOOT_MAVEN = "org.springframework.boot:spring-boot"
    const val SPRING_BOOT_ACTUATOR_MAVEN = "org.springframework.boot:spring-boot-actuator"

}
