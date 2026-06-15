/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.initializr

import com.explyt.spring.initializr.SpringInitConst.EXPLYT_SPRING_INITIALIZR_DESCRIPTION
import com.explyt.spring.initializr.SpringInitConst.EXPLYT_SPRING_INITIALIZR_ID
import com.explyt.spring.initializr.SpringInitConst.EXPLYT_SPRING_INITIALIZR_NAME
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import javax.swing.Icon


class ExplytSpringInitializrModuleType :
    ModuleType<ExplytSpringInitializrModuleBuilder>(EXPLYT_SPRING_INITIALIZR_ID) {

    companion object {
        fun getInstance(): ExplytSpringInitializrModuleType {
            return ModuleTypeManager.getInstance()
                .findByID(EXPLYT_SPRING_INITIALIZR_ID) as ExplytSpringInitializrModuleType
        }
    }

    override fun createModuleBuilder(): ExplytSpringInitializrModuleBuilder {
        return ExplytSpringInitializrModuleBuilder()
    }

    override fun getName(): String {
        return EXPLYT_SPRING_INITIALIZR_NAME
    }

    override fun getDescription(): String {
        return EXPLYT_SPRING_INITIALIZR_DESCRIPTION
    }

    override fun getNodeIcon(isOpened: Boolean): Icon {
        return SpringInitIcons.Spring
    }

}