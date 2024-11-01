package com.explyt.spring.core.language.profiles

import com.intellij.ide.presentation.Presentation
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.PomRenameableTarget
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import java.util.*


@Presentation(typeName = "Spring Profile")
class SpringProfileTarget(element: PsiElement, private var name: String, private val nameOffset: Int) :
    PomRenameableTarget<Any> {
    private val elementPointer: SmartPsiElementPointer<PsiElement>

    init {
        val containingFile = element.containingFile
        val project = containingFile?.project ?: element.project
        elementPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(element, containingFile)
    }

    override fun isValid() = elementPointer.element != null

    override fun getName() = name

    override fun isWritable() = true

    override fun setName(newName: String): Any? {
        name = newName
        return null
    }

    override fun navigate(requestFocus: Boolean) {
        val elementRange = elementPointer.range ?: return
        var offset: Int = elementRange.startOffset
        if (nameOffset < elementRange.endOffset - offset) {
            offset += nameOffset
        }
        val virtualFile: VirtualFile? = elementPointer.virtualFile
        if (virtualFile != null && virtualFile.isValid) {
            PsiNavigationSupport.getInstance().createNavigatable(elementPointer.project, virtualFile, offset)
                .navigate(requestFocus)
        }
    }

    override fun canNavigate() = canNavigateToSource()

    override fun canNavigateToSource(): Boolean {
        if (nameOffset < 0) return false
        val element: PsiElement? = elementPointer.element
        return element != null && PsiNavigationSupport.getInstance().canNavigate(element)
    }

    override fun hashCode() = name.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val target = other as SpringProfileTarget
        return Objects.equals(name, target.name)
    }
}