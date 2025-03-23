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

package com.explyt.spring.core.tracker

import com.intellij.java.library.JavaLibraryModificationTracker
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.ModificationTracker.NEVER_CHANGED
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.intellij.psi.impl.PsiTreeChangeEventImpl.PsiEventType.*
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.*
import org.jetbrains.uast.util.ClassSet
import org.jetbrains.uast.util.isInstanceOf
import org.jetbrains.yaml.psi.YAMLFile


@Suppress("UnstableApiUsage")
class ExplytModelModificationTracker(project: Project) : SimpleModificationTracker() {
    private val javaLibraryTracker: ModificationTracker = JavaLibraryModificationTracker.getInstance(project)

    override fun getModificationCount(): Long {
        return super.getModificationCount() + javaLibraryTracker.modificationCount
    }
}

@Suppress("UnstableApiUsage")
class ExplytAnnotationModificationTracker(project: Project) : SimpleModificationTracker() {
    private val javaLibraryTracker: ModificationTracker = JavaLibraryModificationTracker.getInstance(project)

    fun getOnlyAnnotationModificationCount(): Long {
        return super.getModificationCount()
    }

    override fun getModificationCount(): Long {
        return super.getModificationCount() + javaLibraryTracker.modificationCount
    }
}

@Suppress("UnstableApiUsage")
class SpringBootExternalSystemTracker(project: Project) : SimpleModificationTracker() {
    private val javaLibraryTracker: ModificationTracker = JavaLibraryModificationTracker.getInstance(project)

    override fun getModificationCount(): Long {
        return super.getModificationCount() + javaLibraryTracker.modificationCount
    }
}

@Suppress("UnstableApiUsage")
class ExplytPropertyModificationTracker(project: Project) : SimpleModificationTracker() {
    private val javaLibraryTracker: ModificationTracker = JavaLibraryModificationTracker.getInstance(project)

    override fun getModificationCount(): Long {
        return super.getModificationCount() + javaLibraryTracker.modificationCount
    }
}

internal class MyUastPsiTreeChangeAdapter(
    private val project: Project,
    private val modelTracker: ExplytModelModificationTracker,
    private val annotationTracker: ExplytAnnotationModificationTracker,
    private val propertyTracker: ExplytPropertyModificationTracker,
    private val refreshFloatingAnnotationTracker: SimpleModificationTracker,
) : PsiTreeChangeAdapter() {
    private val uastPsiPossibleTypes = HashMap<String, CachedValue<UastPsiPossibleTypes>>()
    private val beforeChildAddRemoveSet = setOf(BEFORE_CHILD_ADDITION, BEFORE_CHILD_REMOVAL)

    override fun beforeChildAddition(event: PsiTreeChangeEvent) {
        processChange(event, event.parent, event.child)
    }

    override fun childrenChanged(event: PsiTreeChangeEvent) {
        if (event is PsiTreeChangeEventImpl && event.isGenericChange) return
        processChange(event, event.parent, event.child)
    }

    override fun beforeChildRemoval(event: PsiTreeChangeEvent) {
        processChange(event, event.parent, event.child)
    }

    override fun childAdded(event: PsiTreeChangeEvent) {
        processChange(event, event.parent, event.child)
    }

    override fun childRemoved(event: PsiTreeChangeEvent) {
        processChange(event, event.parent, event.child)
    }

    override fun childReplaced(event: PsiTreeChangeEvent) {
        processChange(event, event.parent, event.oldChild)
    }

    private fun processChange(event: PsiTreeChangeEvent, parent: PsiElement?, child: PsiElement?) {
        val psiFile = event.file
        if (psiFile is PropertiesFile) {
            modelTracker.incModificationCount()
            propertyTracker.incModificationCount()
            return
        }
        if (psiFile is YAMLFile) {
            modelTracker.incModificationCount()
            propertyTracker.incModificationCount()
            return
        }
        if (psiFile !is PsiClassOwner) {
            return
        }
        val languageId = psiFile.language.id

        // do not load file content on file creation (`VirtualFileListener` will signal itself)
        if (parent is PsiFile && child == null) {
            return
        }
        if (event.newChild is PsiWhiteSpace && event.oldChild is PsiWhiteSpace) {
            return
        }

        val eventType = (event as? PsiTreeChangeEventImpl)?.code
        //added or removed class or method or field
        if (eventType != null && beforeChildAddRemoveSet.contains(eventType) && isClassOrMethodOrField(child)) {
            modelTracker.incModificationCount()
            return
        }
        //added or removed comment
        if (eventType == CHILD_REPLACED && isClassOrMethodCommented(event)) {
            modelTracker.incModificationCount()
            return
        }

        if (eventType == CHILD_REPLACED && ((event.newChild is PsiClass && event.oldChild is PsiClass)
                    || (event.newChild is KtClassBody && event.oldChild is KtClassBody))
        ) {
            modelTracker.incModificationCount()
            return
        }

        val possiblePsiTypes = getPossiblePsiTypesFor(languageId) ?: return
        val newChild = event.newChild
        val grandParent = parent?.parent
        var unsafeGrandChild: PsiElement? = null
        if (child !is LazyParseablePsiElement) {
            try {
                unsafeGrandChild = child?.firstChild
            } catch (ignored: Exception) {
            }
        }
        if (isRelevantAnnotation(child, possiblePsiTypes) // removed annotation
            || isRelevantAnnotation(unsafeGrandChild, possiblePsiTypes) // removed annotation
            || isRelevantAnnotation(newChild, possiblePsiTypes) // added   annotation
            || getFirstParentIsRelevantAnnotation(parent, possiblePsiTypes) != null // change in  annotation
        ) {
            //tracker for root package search
            modelTracker.incModificationCount()
            if (isNotTest(psiFile)) {
                annotationTracker.incModificationCount()
                //if (checkIsSpringReloadAnnotation(eventType, child, parent, grandParent, unsafeGrandChild)) {
                refreshFloatingAnnotationTracker.incModificationCount()
                //}
            }
            return
        }
        if (((grandParent.isInstanceOf(possiblePsiTypes.forClasses) // modifier changed (static, public)
                    && !parent.isInstanceOf(possiblePsiTypes.forAnnotationOwners))
                    || innerClassChanged(parent, grandParent, child, event, possiblePsiTypes)
                    || classRename(parent, event.newChild, event.oldChild, possiblePsiTypes)
                    || methodRename(parent, event.newChild, event.oldChild, possiblePsiTypes)
                    || fieldRename(parent, event.newChild, event.oldChild, possiblePsiTypes)
                    || parameterRename(parent, event.newChild, event.oldChild, possiblePsiTypes)
                    || parameterAddedOrRemove(grandParent, event.newChild, event.oldChild, possiblePsiTypes)
                    || changedReturnStatement(newChild, parent, grandParent, possiblePsiTypes))
            || child is LazyParseablePsiElement
        ) {
            modelTracker.incModificationCount()
        }
    }

    private fun checkIsSpringReloadAnnotation(
        eventType: PsiTreeChangeEventImpl.PsiEventType?,
        child: PsiElement?, parent: PsiElement?, grandParent: PsiElement?, unsafeGrandChild: PsiElement?
    ): Boolean {
        eventType ?: return false
        if (eventType != CHILD_ADDED && eventType != CHILD_REPLACED) return false
        val qualifiedName = getAnnotationQualifiedName(child, parent, grandParent, unsafeGrandChild) ?: return false
        return (qualifiedName.startsWith("org.spring")
                && !qualifiedName.contains("Autowired")
                && !qualifiedName.contains("Qualifier")
                && !qualifiedName.contains("Value")
                && !qualifiedName.contains("Lazy")
                && !qualifiedName.contains("Primary"))
    }

    private fun getAnnotationQualifiedName(
        child: PsiElement?,
        parent: PsiElement?,
        grandParent: PsiElement?,
        unsafeGrandChild: PsiElement?
    ): String? {
        return try {
            var uAnnotation = toUElementWithCatch(child)
            if (uAnnotation == null) {
                uAnnotation = toUElementWithCatch(parent)
            }
            if (uAnnotation == null) {
                uAnnotation = toUElementWithCatch(grandParent)
            }
            if (uAnnotation == null) {
                uAnnotation = toUElementWithCatch(unsafeGrandChild)
            }
            if (uAnnotation == null) {
                uAnnotation = findAnnotationInParent(child)
            }
            if (uAnnotation == null) {
                uAnnotation = findAnnotationInParent(parent)
            }
            uAnnotation?.qualifiedName
        } catch (e: Exception) {
            null
        }
    }

    private fun toUElementWithCatch(child: PsiElement?): UAnnotation? {
        return try {
            child?.toUElement() as? UAnnotation
        } catch (e: Exception) {
            null
        }
    }

    private fun findAnnotationInParent(psiElement: PsiElement?): UAnnotation? {
        try {
            var uElement = psiElement.toUElement() ?: return null
            for (i in 0..10) {
                if (uElement is UAnnotation) return uElement
                uElement = uElement.uastParent ?: return null
            }
            return null
        } catch (e: Exception) {
            return null
        }
    }

    private fun isNotTest(psiFile: PsiClassOwner): Boolean {
        return !ProjectRootManager.getInstance(this.project).fileIndex.isInTestSourceContent(psiFile.virtualFile)
    }

    private fun changedReturnStatement(
        newChild: PsiElement?, parent: PsiElement?, grandParent: PsiElement?, possiblePsiTypes: UastPsiPossibleTypes
    ) = newChild.isInstanceOf(possiblePsiTypes.forReturn)
            || parent.isInstanceOf(possiblePsiTypes.forReturn)
            || grandParent.isInstanceOf(possiblePsiTypes.forReturn)

    private fun classRename(
        parent: PsiElement?, newChild: PsiElement?, oldChild: PsiElement?, possiblePsiTypes: UastPsiPossibleTypes
    ): Boolean {
        return parent.isInstanceOf(possiblePsiTypes.forClasses)
                && newChild.isInstanceOf(possiblePsiTypes.forIdentifier)
                && oldChild.isInstanceOf(possiblePsiTypes.forIdentifier)
    }

    private fun methodRename(
        parent: PsiElement?, newChild: PsiElement?, oldChild: PsiElement?, possiblePsiTypes: UastPsiPossibleTypes
    ): Boolean {
        return parent.isInstanceOf(possiblePsiTypes.forMethods)
                && newChild.isInstanceOf(possiblePsiTypes.forIdentifier)
                && oldChild.isInstanceOf(possiblePsiTypes.forIdentifier)
    }

    private fun fieldRename(
        parent: PsiElement?, newChild: PsiElement?, oldChild: PsiElement?, possiblePsiTypes: UastPsiPossibleTypes
    ): Boolean {
        return parent.isInstanceOf(possiblePsiTypes.forFields)
                && newChild.isInstanceOf(possiblePsiTypes.forIdentifier)
                && oldChild.isInstanceOf(possiblePsiTypes.forIdentifier)
    }

    private fun parameterRename(
        parent: PsiElement?, newChild: PsiElement?, oldChild: PsiElement?, possiblePsiTypes: UastPsiPossibleTypes
    ): Boolean {
        return parent.isInstanceOf(possiblePsiTypes.forParameters)
                && newChild.isInstanceOf(possiblePsiTypes.forIdentifier)
                && oldChild.isInstanceOf(possiblePsiTypes.forIdentifier)
    }

    private fun parameterAddedOrRemove(
        grandParent: PsiElement?,
        newChild: PsiElement?,
        oldChild: PsiElement?,
        possiblePsiTypes: UastPsiPossibleTypes
    ): Boolean {
        return grandParent.isInstanceOf(possiblePsiTypes.forMethods)
                && (
                newChild.isInstanceOf(possiblePsiTypes.forParameters)
                        || oldChild.isInstanceOf(possiblePsiTypes.forParameters)
                )
    }

    private fun innerClassChanged(
        parent: PsiElement?,
        grandParent: PsiElement?,
        child: PsiElement?,
        event: PsiTreeChangeEvent,
        possiblePsiTypes: UastPsiPossibleTypes,
    ): Boolean {
        // added/removed inner class
        return ((parent.isInstanceOf(possiblePsiTypes.forClasses)
                || grandParent.isInstanceOf(possiblePsiTypes.forClasses))
                && (child.isInstanceOf(possiblePsiTypes.forClasses)
                || event.newChild.isInstanceOf(possiblePsiTypes.forClasses)
                || event.oldChild.isInstanceOf(possiblePsiTypes.forClasses)))
    }

    private fun getFirstParentIsRelevantAnnotation(
        parent: PsiElement?,
        possiblePsiTypes: UastPsiPossibleTypes
    ) = PsiTreeUtil.findFirstParent(parent) { isRelevantAnnotation(it, possiblePsiTypes) }

    private fun isRelevantAnnotation(
        psiElement: PsiElement?,
        possiblePsiTypes: UastPsiPossibleTypes
    ): Boolean {
        if (!psiElement.isInstanceOf(possiblePsiTypes.forAnnotations)) {
            return false
        }
        val modifierListOwner = PsiTreeUtil.findFirstParent(psiElement) {
            it.isInstanceOf(possiblePsiTypes.forAnnotationOwners) && !it.isInstanceOf(possiblePsiTypes.forAnnotations)
        }
        return (modifierListOwner == null // just added annotation
                || modifierListOwner.isInstanceOf(possiblePsiTypes.forClasses)
                || modifierListOwner.isInstanceOf(possiblePsiTypes.forFields)
                || (modifierListOwner.isInstanceOf(possiblePsiTypes.forMethods)
                && !modifierListOwner.isInstanceOf(possiblePsiTypes.forVariables)))
    }

    private fun getPossiblePsiTypesFor(languageId: String): UastPsiPossibleTypes? {
        val uastPsiTypeCache = uastPsiPossibleTypes[languageId]
        if (uastPsiTypeCache != null) return uastPsiTypeCache.value
        val possibleTypesCachedValue = CachedValuesManager.getManager(project).createCachedValue {
            val uastLanguagePlugin = UastLanguagePlugin.Companion.getInstances().find { languageId == it.language.id }
                ?: return@createCachedValue null
            CachedValueProvider.Result.create(UastPsiPossibleTypes(uastLanguagePlugin), NEVER_CHANGED)
        }
        uastPsiPossibleTypes[languageId] = possibleTypesCachedValue
        return possibleTypesCachedValue.value
    }

    private fun isClassOrMethodOrField(child: PsiElement?): Boolean {
        return child is PsiClass || child is PsiMethod || child is KtFunction
                || child is PsiField || child is KtProperty
    }

    private fun isClassOrMethodCommented(event: PsiTreeChangeEvent): Boolean {
        val newChild = event.newChild ?: return false
        val oldChild = event.oldChild ?: return false
        return (newChild is PsiComment && isClassOrMethodOrField(oldChild))
                || (oldChild is PsiComment && isClassOrMethodOrField(newChild))
    }
}

internal class SpringVirtualFileListener(
    val project: Project, private val tracker: SimpleModificationTracker
) : VirtualFileListener {
    private val projectFileIndex = ProjectFileIndex.getInstance(project)

    override fun fileCreated(event: VirtualFileEvent) {
        incModificationCountIfMine(event)
    }

    override fun beforeFileDeletion(event: VirtualFileEvent) {
        incModificationCountIfMine(event)
    }

    override fun fileMoved(event: VirtualFileMoveEvent) {
        incModificationCountIfMine(event)
    }

    // file is rename
    override fun propertyChanged(event: VirtualFilePropertyEvent) {
        if (event.propertyName == VirtualFile.PROP_NAME) incModificationCountIfMine(event)
    }

    private fun incModificationCountIfMine(event: VirtualFileEvent) {
        val file = event.file
        if (!projectFileIndex.isInContent(file)) {
            return
        }
        if (!file.isDirectory && (projectFileIndex.isExcluded(file) || projectFileIndex.isUnderIgnored(file))) {
            return
        }
        tracker.incModificationCount()
    }
}

internal class UastPsiPossibleTypes(uastPlugin: UastLanguagePlugin) {
    val forClasses: ClassSet<PsiElement>
    val forMethods: ClassSet<PsiElement>
    val forFields: ClassSet<PsiElement>
    val forVariables: ClassSet<PsiElement>
    val forReturn: ClassSet<PsiElement>
    val forIdentifier: ClassSet<PsiElement>
    val forParameters: ClassSet<PsiElement>
    val forAnnotations: ClassSet<PsiElement>
    val forAnnotationOwners: ClassSet<PsiElement>

    init {
        forClasses = uastPlugin.getPossiblePsiSourceTypes(UClass::class.java)
        forMethods = uastPlugin.getPossiblePsiSourceTypes(UMethod::class.java)
        forFields = uastPlugin.getPossiblePsiSourceTypes(UField::class.java)
        forVariables = uastPlugin.getPossiblePsiSourceTypes(UVariable::class.java)
        forParameters = uastPlugin.getPossiblePsiSourceTypes(UParameter::class.java)
        forIdentifier = uastPlugin.getPossiblePsiSourceTypes(UIdentifier::class.java)
        forReturn = uastPlugin.getPossiblePsiSourceTypes(UReturnExpression::class.java)
        forAnnotations = uastPlugin.getPossiblePsiSourceTypes(UAnnotation::class.java)
        forAnnotationOwners = uastPlugin.getPossiblePsiSourceTypes(UAnnotated::class.java)
    }
}

