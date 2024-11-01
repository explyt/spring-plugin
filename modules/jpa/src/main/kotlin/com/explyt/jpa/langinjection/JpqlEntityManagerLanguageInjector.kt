package com.explyt.jpa.langinjection

import com.explyt.jpa.JpaClasses
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.*

class JpqlEntityManagerLanguageInjector : JpqlInjectorBase() {
    override fun isValidPlace(uElement: UElement): Boolean {
        val uCallExpression = uElement.getParentOfType<UCallExpression>() ?: return false

        val firstParameter = uCallExpression.valueArguments.getOrNull(0) ?: return false

        if (!uElement.isUastChildOf(firstParameter))
            return false

        val method = uCallExpression.tryResolve() as? PsiMethod
            ?: return false

        if (!JpaClasses.entityManager.check(method.containingClass?.qualifiedName))
            return false

        return method.name == "createQuery"
    }
}