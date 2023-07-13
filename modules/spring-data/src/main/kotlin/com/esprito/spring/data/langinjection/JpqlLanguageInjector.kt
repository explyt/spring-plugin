package com.esprito.spring.data.langinjection

import com.esprito.jpa.ql.JpqlLanguage
import com.esprito.spring.data.SpringDataClasses
import com.intellij.openapi.util.TextRange
import com.intellij.psi.InjectedLanguagePlaces
import com.intellij.psi.LanguageInjector
import com.intellij.psi.PsiLanguageInjectionHost
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.expressions.UInjectionHost
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElementOfType

class JpqlLanguageInjector : LanguageInjector {
    override fun getLanguagesToInject(
        host: PsiLanguageInjectionHost,
        injectionPlacesRegistrar: InjectedLanguagePlaces
    ) {
        val uInjectionHost = host.toUElementOfType<UInjectionHost>()
            ?: return

        val parentAnnotation = uInjectionHost.getParentOfType<UAnnotation>()
            ?.takeIf { it.qualifiedName == SpringDataClasses.QUERY }
            ?: return

        if (
            parentAnnotation
                .findAttributeValue("nativeQuery")
                ?.evaluate() == true
        ) {
            return
        }

        injectionPlacesRegistrar.addPlace(
            JpqlLanguage.INSTANCE,
            TextRange(1, host.textLength - 1),
            "",
            ""
        )
    }
}