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

package com.explyt.spring.core.action

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.util.ExplytPsiUtil.isPublic
import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.generation.actions.BaseGenerateAction
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.endOffset
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement

class SpringBootAnalyzeRunGenerateAction : BaseGenerateAction(GenerateSpringBootRunHandler()) {
    init {
        getTemplatePresentation().text = SpringCoreBundle.message("explyt.spring.action.bean.analyzer")
    }

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        if (file.language == KotlinLanguage.INSTANCE) {
            if (!file.isWritable || file !is KtFile || file.isCompiled) return false
        } else {
            if (!file.isWritable || !super.isValidForFile(project, editor, file)) return false
        }

        if (!SpringCoreUtil.isSpringBootProject(project)) return false

        val elementAtCaret = file.findElementAt(editor.caretModel.offset)
            ?.let { if (it is PsiWhiteSpace) it.parent else it } ?: return false

        val uElement = elementAtCaret.toUElement() ?: return false
        val uMethod = uElement.getParentOfType<UMethod>() ?: return false
        if (uMethod.getContainingUClass()?.methods?.any { it.name == EXPLYT_APPLICATION_RUN } == true) return false
        if (!uMethod.javaPsi.isValid) return false
        return uMethod.isPublic && uMethod.name == "main" && uMethod.uastParameters.size == 1
    }
}

private class GenerateSpringBootRunHandler() : CodeInsightActionHandler {
    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        StatisticService.getInstance().addActionUsage(StatisticActionId.GENERATE_SPRING_BOOT_BEAN_ANALYZE)
        val module = ModuleUtil.findModuleForPsiElement(file) ?: return

        runWriteAction {
            val offset = getOffset(editor, file) ?: return@runWriteAction
            editor.caretModel.moveToOffset(offset)

            val documentManager = PsiDocumentManager.getInstance(project)
            val document = documentManager.getDocument(file) ?: return@runWriteAction
            PsiDocumentManager.getInstance(file.project).commitDocument(document)

            val template = getTemplate(module, file) ?: return@runWriteAction
            KotlinMethodGenerateUtils.startTemplate(project, editor, template)
        }
    }

    private fun getOffset(editor: Editor, file: PsiFile): Int? {
        val currentOffset = editor.caretModel.offset
        val psiMethod = PsiTreeUtil.findElementOfClassAtOffset(file, currentOffset - 1, PsiMethod::class.java, false)
            ?: PsiTreeUtil.findElementOfClassAtOffset(file, currentOffset - 1, KtFunction::class.java, false)
        return psiMethod?.endOffset
    }

    private fun getTemplate(module: Module, psiElement: PsiElement): Template? {
        val template = TemplateManager.getInstance(module.project).createTemplate("", "")
        template.addTextSegment(
            """
            /**
            * Do not rename this method!
            */   
        """.trimIndent()
        )
        if (psiElement.language == KotlinLanguage.INSTANCE) {
            template.addTextSegment(kotlinMethod.trimIndent())
        } else if (psiElement.language == JavaLanguage.INSTANCE) {
            template.addTextSegment(javaMethod.trimIndent())
        } else {
            return null
        }
        template.setToIndent(true)
        template.isToReformat = true
        template.isToShortenLongNames = true
        return template
    }

}

const val EXPLYT_APPLICATION_RUN = "explytApplicationRun"

private const val kotlinMethod = """                                          
    private fun $EXPLYT_APPLICATION_RUN(
        args: Array<String>, vararg primarySources: Class<*>, supplier: () -> org.springframework.context.ConfigurableApplicationContext
    ): org.springframework.context.ConfigurableApplicationContext {
        val starterClass: Class<*>
        try {
            starterClass = Class.forName("com.explyt.spring.boot.bean.reader.SpringBootBeanReaderStarter")
        } catch (_: Exception) {
            return supplier.invoke()
        }
        val method = starterClass.methods.first { it.name == "wrapToExplytBeanAnalyze" }
        return method.invoke(null, primarySources.toList(), args) as org.springframework.context.ConfigurableApplicationContext
    }   
        """

private const val javaMethod = """           
    private static org.springframework.context.ConfigurableApplicationContext $EXPLYT_APPLICATION_RUN(
            java.util.List<Class<?>> primarySources, String[] args, java.util.function.Supplier<ConfigurableApplicationContext> supplier
    ) {
        Class<?> starterClass;
        try {
            starterClass = Class.forName("com.explyt.spring.boot.bean.reader.SpringBootBeanReaderStarter");
        } catch (Exception e) {
            return supplier.get();
        }
        java.lang.reflect.Method method = java.util.Arrays.stream(starterClass.getMethods())
                .filter(it -> it.getName().equals("wrapToExplytBeanAnalyze")).findFirst().orElse(null);
        try {
            return (ConfigurableApplicationContext) method.invoke(null, primarySources, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
        """