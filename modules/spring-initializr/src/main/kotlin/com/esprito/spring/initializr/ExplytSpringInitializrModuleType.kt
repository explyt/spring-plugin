package com.esprito.spring.initializr

import com.esprito.spring.initializr.SpringInitConst.ESPRITO_SPRING_INITIALIZR_DESCRIPTION
import com.esprito.spring.initializr.SpringInitConst.ESPRITO_SPRING_INITIALIZR_ID
import com.esprito.spring.initializr.SpringInitConst.ESPRITO_SPRING_INITIALIZR_NAME
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import javax.swing.Icon


class ExplytSpringInitializrModuleType :
    ModuleType<ExplytSpringInitializrModuleBuilder>(ESPRITO_SPRING_INITIALIZR_ID) {

    companion object {
        fun getInstance(): ExplytSpringInitializrModuleType {
            return ModuleTypeManager.getInstance()
                .findByID(ESPRITO_SPRING_INITIALIZR_ID) as ExplytSpringInitializrModuleType
        }
    }

    override fun createModuleBuilder(): ExplytSpringInitializrModuleBuilder {
        return ExplytSpringInitializrModuleBuilder()
    }

    override fun getName(): String {
        return ESPRITO_SPRING_INITIALIZR_NAME
    }

    override fun getDescription(): String {
        return ESPRITO_SPRING_INITIALIZR_DESCRIPTION
    }

    override fun getNodeIcon(isOpened: Boolean): Icon {
        return SpringInitIcons.Spring
    }

}