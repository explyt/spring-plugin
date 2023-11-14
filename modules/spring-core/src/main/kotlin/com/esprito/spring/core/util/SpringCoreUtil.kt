package com.esprito.spring.core.util

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.JavaEeClasses
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.language.injection.ConfigurationPropertiesInjector
import com.esprito.spring.core.properties.SpringPropertySourceSearch
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.util.EspritoAnnotationUtil.getStringMemberValues
import com.esprito.util.EspritoAnnotationUtil.getStringValue
import com.esprito.util.EspritoPsiUtil.deepPsiClassType
import com.esprito.util.EspritoPsiUtil.findChildrenOfType
import com.esprito.util.EspritoPsiUtil.getMetaAnnotation
import com.esprito.util.EspritoPsiUtil.isCollection
import com.esprito.util.EspritoPsiUtil.isEqualOrInheritor
import com.esprito.util.EspritoPsiUtil.isInterface
import com.esprito.util.EspritoPsiUtil.isMap
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedByOrSelf
import com.esprito.util.EspritoPsiUtil.isOptional
import com.esprito.util.EspritoPsiUtil.isString
import com.esprito.util.EspritoPsiUtil.resolvedPsiClass
import com.esprito.util.EspritoPsiUtil.returnPsiClass
import com.esprito.util.ModuleUtil
import com.esprito.util.runReadNonBlocking
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.FileContextUtil
import com.intellij.psi.util.PsiUtil
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
                && !psiClass.hasModifierProperty(PsiModifier.PRIVATE)
                && !psiClass.isAnnotationType
                && psiClass.qualifiedName != null
                && !PsiUtil.isLocalOrAnonymousClass(psiClass)
    }

    // TODO: value or basePackages
    fun existComponentScan(module: Module): Boolean =
        SpringSearchService.getInstance(module.project)
            .getAllComponentScanBeans(module, SpringCoreClasses.COMPONENT_SCAN)
            .any { !it.isAnnotationType }

    fun PsiClass.getBeanName(): String? =
        name?.replaceFirstChar { it.lowercase(Locale.getDefault()) }

    fun PsiClass.resolveBeanName(module: Module): String {
        return resolveBeanNameByPsiAnnotations(
            SpringSearchService.getInstance(module.project).getComponentClassAnnotations(module)
        )
    }

    fun PsiClass.resolveBeanNameByPsiAnnotations(annotationPsiClasses: Collection<PsiClass>): String {
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
            if (isMap && isInterface && parameterCount == 2 && parameters[0].isString) {
                // Map<String, Bean>
                return parameters[1]
            }
            return this
        }

    fun PsiClass?.canResolveBeanClass(targetClasses: Set<PsiClass>): Boolean =
        this != null && targetClasses.any { it == this }

    fun PsiType.canResolveBeanClass(targetClasses: Set<PsiClass>): Boolean {
        val beanPsiType = beanPsiType
        return when (beanPsiType) {
            is PsiClassType -> beanPsiType.resolvedPsiClass.canResolveBeanClass(targetClasses)
            is PsiWildcardType -> {
                if (!beanPsiType.isBounded) {
                    return true
                }
                targetClasses.any { it.matchesWildcardType(beanPsiType) }
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

    fun PsiModifierListOwner.resolveBeanNameByAnnotations(annotationNames: Collection<String>): String? {
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
        if (this !is PsiClassType) {
            return false
        }
        if (beanPsiType is PsiClassType) {
            return this.isEqualOrInheritor(beanPsiType)
                    && (beanPsiType.parameters.isEmpty() || this.equalParamsWithBound(beanPsiType))
        }
        if (beanPsiType is PsiWildcardType) {
            return this.resolvedPsiClass?.matchesWildcardType(beanPsiType) ?: return false
        }
        return false
    }

    fun PsiClassType.equalParamsWithBound(otherPsiType: PsiClassType): Boolean {
        if (parameters.size != otherPsiType.parameters.size) {
            return false
        }
        return this.parameters.withIndex().all { (index, value) ->
            otherPsiType.parameters[index].let { it == value || !it.isBounded() }
        }
    }

    fun PsiClass.matchesWildcardType(beanPsiType: PsiWildcardType): Boolean {
        if (beanPsiType.isExtends) {
            val extendsBeanPsiClass = beanPsiType.extendsBound.resolvedPsiClass
            if (extendsBeanPsiClass != null) {
                return this.isEqualOrInheritor(extendsBeanPsiClass)
            }
        }
        if (beanPsiType.isSuper) {
            val superBeanPsiClass = beanPsiType.superBound.resolvedPsiClass
            if (superBeanPsiClass != null) {
                return superBeanPsiClass == this
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

        return sequence.filter { psiMethod ->
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

}
