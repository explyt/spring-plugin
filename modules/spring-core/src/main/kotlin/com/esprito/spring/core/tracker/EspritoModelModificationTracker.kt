package com.esprito.spring.core.tracker

import com.intellij.java.library.JavaLibraryModificationTracker
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.ModificationTracker.NEVER_CHANGED
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.*
import com.intellij.openapi.vfs.VirtualFileManager.VFS_CHANGES
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter
import com.intellij.psi.*
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.*
import org.jetbrains.uast.util.ClassSet
import org.jetbrains.uast.util.isInstanceOf


@Suppress("UnstableApiUsage")
class EspritoModelModificationTracker(project: Project, parent: Disposable) : SimpleModificationTracker() {
    val javaLibraryTracker: ModificationTracker = JavaLibraryModificationTracker.getInstance(project)

    init {
        PsiManager.getInstance(project).addPsiTreeChangeListener(MyUastPsiTreeChangeAdapter(project, this), parent)
        project.messageBus.connect(parent)
            .subscribe(VFS_CHANGES, BulkVirtualFileListenerAdapter(SpringVirtualFileListener(project, this)))
    }

    override fun getModificationCount(): Long {
        return super.getModificationCount() + javaLibraryTracker.modificationCount
    }
}

internal class MyUastPsiTreeChangeAdapter(
    val project: Project, val tracker: SimpleModificationTracker
) : PsiTreeChangeAdapter() {
    private val uastPsiPossibleTypes = HashMap<String, CachedValue<UastPsiPossibleTypes>>()

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
            tracker.incModificationCount()
            return
        }
        val languageId = psiFile?.language?.id ?: return
        if ("yaml" == languageId) {
            tracker.incModificationCount()
            return
        }
        if (psiFile !is PsiClassOwner) {
            return
        }

        // do not load file content on file creation (`VirtualFileListener` will signal itself)
        if (parent is PsiFile && child == null) {
            return
        }
        if (event.newChild is PsiWhiteSpace && event.oldChild is PsiWhiteSpace) {
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
        if ((isRelevantAnnotation(child, possiblePsiTypes) // removed annotation
                    || isRelevantAnnotation(unsafeGrandChild, possiblePsiTypes) // removed annotation
                    || isRelevantAnnotation(newChild, possiblePsiTypes) // added   annotation
                    || (grandParent.isInstanceOf(possiblePsiTypes.forClasses) // modifier changed (static, public)
                    && !parent.isInstanceOf(possiblePsiTypes.forAnnotationOwners))
                    || innerClassChanged(parent, grandParent, child, event, possiblePsiTypes)
                    || classRename(parent, event.newChild, event.oldChild, possiblePsiTypes)
                    || methodRename(parent, event.newChild, event.oldChild, possiblePsiTypes)
                    || fieldRename(parent, event.newChild, event.oldChild, possiblePsiTypes)
                    || parameterRename(parent, event.newChild, event.oldChild, possiblePsiTypes)
                    || parameterStartInput(grandParent, event.newChild, possiblePsiTypes)
                    || changedReturnStatement(newChild, parent, grandParent, possiblePsiTypes)
                    || getFirstParentIsRelevantAnnotation(parent, possiblePsiTypes) != null)
            || child is LazyParseablePsiElement
        ) {
            tracker.incModificationCount()
        }
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

    private fun parameterStartInput(
        grandParent: PsiElement?, newChild: PsiElement?, possiblePsiTypes: UastPsiPossibleTypes
    ): Boolean {
        return grandParent.isInstanceOf(possiblePsiTypes.forMethods)
                && newChild.isInstanceOf(possiblePsiTypes.forParameters)
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

