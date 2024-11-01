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

package com.explyt.spring.core.runconfiguration.edit

import com.explyt.spring.core.SpringRunConfigurationBundle
import com.explyt.spring.core.runconfiguration.SpringBootRunConfiguration
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.JavaExecutionUtil
import com.intellij.execution.application.ClassEditorField
import com.intellij.execution.application.JavaSettingsEditorBase
import com.intellij.execution.configurations.ConfigurationUtil
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.ui.*
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Predicates
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.PsiMethodUtil
import com.intellij.ui.EditorTextField
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.util.indexing.DumbModeAccessType
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.text.nullize
import com.intellij.util.ui.UIUtil
import java.awt.event.ItemEvent

class SpringBootConfigurationEditor(
    configuration: SpringBootRunConfiguration
) :
    JavaSettingsEditorBase<SpringBootRunConfiguration>(
        configuration
    ) {

    override fun customizeFragments(
        fragments: MutableList<SettingsEditorFragment<SpringBootRunConfiguration, *>>,
        moduleClasspath: SettingsEditorFragment<SpringBootRunConfiguration, ModuleClasspathCombo>,
        commonParameterFragments: CommonParameterFragments<SpringBootRunConfiguration>
    ) {
        fragments.add(commonParameterFragments.programArguments())
        fragments.add(TargetPathFragment())
        fragments.add(commonParameterFragments.createRedirectFragment())
        val mainClassFragment: SettingsEditorFragment<SpringBootRunConfiguration, EditorTextField> =
            createMainClass(moduleClasspath.component())
        fragments.add(mainClassFragment)
        val jreSelector =
            DefaultJreSelector.fromSourceRootsDependencies(moduleClasspath.component(), mainClassFragment.component())
        val jrePath = CommonJavaFragments.createJrePath<SpringBootRunConfiguration>(jreSelector)
        fragments.add(jrePath)
        fragments.add(createShortenClasspath(moduleClasspath.component(), jrePath, true))
        fragments.add(createActiveProfilesFragment(moduleClasspath))
    }

    private fun createActiveProfilesFragment(
        moduleClasspath: SettingsEditorFragment<SpringBootRunConfiguration, ModuleClasspathCombo>
    ): SettingsEditorFragment<SpringBootRunConfiguration, *> {
        val completionProvider = ProfilesCompletionProvider()
        val textField: TextFieldWithAutoCompletion<*> = TextFieldWithAutoCompletion(
            mySettings.project,
            completionProvider,
            true,
            ""
        )

        completionProvider.module = mySettings.modules.firstOrNull()

        val moduleClasspathCombo = moduleClasspath.component()
        val listener: (e: ItemEvent) -> Unit = {
            completionProvider.module = moduleClasspathCombo.selectedModule
        }
        moduleClasspathCombo.addItemListener(listener)
        Disposer.register(this) {
            moduleClasspathCombo.removeItemListener(listener)
        }

        val fragment = SettingsEditorFragment<SpringBootRunConfiguration, LabeledComponent<EditorTextField>>(
            "com.explyt.spring.runconfiguration.profiles",
            SpringRunConfigurationBundle.message("run.configuration.boot.editor.profiles"),
            SpringRunConfigurationBundle.message("run.configuration.boot.editor.spring.group"),
            LabeledComponent.create(
                textField,
                SpringRunConfigurationBundle.message("run.configuration.boot.editor.profiles.label"),
                "West"
            ),
            { configuration: SpringBootRunConfiguration, field ->
                field.component.text = configuration.springProfiles ?: ""
            },
            { configuration: SpringBootRunConfiguration, field ->
                configuration.springProfiles = field.component.text.nullize(true)
            },
            Predicates.alwaysTrue()
        )
        fragment.isRemovable = false
        fragment.setHint("Profiles comma separated list")
        fragment.setEditorGetter { field ->
            val editor = field.component.editor
            editor?.contentComponent ?: field
        }
        return fragment
    }

    private fun createMainClass(classpathCombo: ModuleClasspathCombo): SettingsEditorFragment<SpringBootRunConfiguration, EditorTextField> {
        val moduleSelector = ConfigurationModuleSelector(project, classpathCombo)
        val mainClass: EditorTextField = ClassEditorField.createClassField(
            project,
            { classpathCombo.selectedModule },
            getVisibilityChecker(moduleSelector), null
        )
        mainClass.background = UIUtil.getTextFieldBackground()
        mainClass.setShowPlaceholderWhenFocused(true)
        CommonParameterFragments.setMonospaced(mainClass)
        val placeholder = ExecutionBundle.message("application.configuration.main.class.placeholder")
        mainClass.setPlaceholder(placeholder)
        mainClass.accessibleContext.accessibleName = placeholder
        CommandLinePanel.setMinimumWidth(mainClass, 300)
        val mainClassFragment = SettingsEditorFragment(
            "mainClass", ExecutionBundle.message("application.configuration.main.class"), null, mainClass, 20,
            { configuration: SpringBootRunConfiguration, component: EditorTextField ->
                component.setText(
                    getQName(configuration.mainClassName)
                )
            },
            { configuration: SpringBootRunConfiguration, component: EditorTextField ->
                val className = component.text
                if (className != configuration.mainClassName) {
                    configuration.mainClassName = getJvmName(className)
                }
            },
            Predicates.alwaysTrue()
        )
        mainClassFragment.setHint(ExecutionBundle.message("application.configuration.main.class.hint"))
        mainClassFragment.isRemovable = false
        mainClassFragment.setEditorGetter { field: EditorTextField ->
            val editor = field.editor
            editor?.contentComponent ?: field
        }
        mainClassFragment.setValidation { configuration: SpringBootRunConfiguration ->
            listOf(
                RuntimeConfigurationException.validate<RuntimeConfigurationException>(
                    mainClass
                ) { configuration.checkClass() }
            )
        }
        return mainClassFragment
    }

    @Suppress("UnstableApiUsage")
    private fun getQName(className: String?): String? {
        if (className == null || className.indexOf('$') < 0) return className
        val psiClass = FileBasedIndex.getInstance().ignoreDumbMode<PsiClass?, RuntimeException>(
            DumbModeAccessType.RAW_INDEX_DATA_ACCEPTABLE
        ) {
            ClassUtil
                .findPsiClass(PsiManager.getInstance(project), className)
        }
        return if (psiClass == null) className else psiClass.qualifiedName
    }

    @Suppress("UnstableApiUsage")
    private fun getJvmName(className: String?): String? {
        return if (className == null) null else FileBasedIndex.getInstance().ignoreDumbMode<String?, RuntimeException>(
            DumbModeAccessType.RELIABLE_DATA_ONLY
        ) {
            val aClass =
                JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))
            if (aClass != null) JavaExecutionUtil.getRuntimeQualifiedName(aClass) else className
        }
    }

    companion object {
        fun getVisibilityChecker(selector: ConfigurationModuleSelector): JavaCodeFragment.VisibilityChecker =
            object : JavaCodeFragment.VisibilityChecker {
                override fun isDeclarationVisible(
                    declaration: PsiElement,
                    place: PsiElement?
                ): JavaCodeFragment.VisibilityChecker.Visibility {
                    if (declaration is PsiClass) {
                        if (ConfigurationUtil.MAIN_CLASS.value(declaration) && PsiMethodUtil.findMainMethod(declaration) != null ||
                            place != null && place.parent != null && selector.findClass(declaration.qualifiedName) != null
                        ) {
                            return JavaCodeFragment.VisibilityChecker.Visibility.VISIBLE
                        }
                    }
                    return JavaCodeFragment.VisibilityChecker.Visibility.NOT_VISIBLE
                }

            }
    }
}