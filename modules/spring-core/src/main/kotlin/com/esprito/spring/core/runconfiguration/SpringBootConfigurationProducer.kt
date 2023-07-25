package com.esprito.spring.core.runconfiguration

import com.esprito.spring.core.util.SpringCoreUtil
import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.application.AbstractApplicationConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

class SpringBootConfigurationProducer : AbstractApplicationConfigurationProducer<SpringBootRunConfiguration>() {
    override fun setupConfigurationFromContext(
        configuration: SpringBootRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {

        if(!SpringCoreUtil.isSpringBootProject(context.project)) {
            return false
        }

        return super.setupConfigurationFromContext(configuration, context, sourceElement)
    }

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
        return other.configuration !is SpringBootRunConfiguration && other.configuration is JavaRunConfigurationBase
    }

    override fun getConfigurationFactory(): ConfigurationFactory = SpringBootConfigurationFactory
}