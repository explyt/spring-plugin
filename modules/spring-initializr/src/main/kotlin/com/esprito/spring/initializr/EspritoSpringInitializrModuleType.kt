package com.esprito.spring.initializr

import com.esprito.spring.initializr.SpringInitConst.ESPRITO_SPRING_INITIALIZR_DESCRIPTION
import com.esprito.spring.initializr.SpringInitConst.ESPRITO_SPRING_INITIALIZR_ID
import com.esprito.spring.initializr.SpringInitConst.ESPRITO_SPRING_INITIALIZR_NAME
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import javax.swing.Icon


class EspritoSpringInitializrModuleType :
    ModuleType<EspritoSpringInitializrModuleBuilder>(ESPRITO_SPRING_INITIALIZR_ID) {

    companion object {
        fun getInstance(): EspritoSpringInitializrModuleType {
            return ModuleTypeManager.getInstance()
                .findByID(ESPRITO_SPRING_INITIALIZR_ID) as EspritoSpringInitializrModuleType
        }
    }

    override fun createModuleBuilder(): EspritoSpringInitializrModuleBuilder {
        return EspritoSpringInitializrModuleBuilder()
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