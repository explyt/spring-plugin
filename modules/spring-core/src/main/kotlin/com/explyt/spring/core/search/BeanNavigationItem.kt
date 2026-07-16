/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.search

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.service.PsiBean
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.pom.Navigatable
import javax.swing.Icon

class BeanNavigationItem(
    private val psiBean: PsiBean,
    private val isActive: Boolean,
    private val navigatable: Navigatable?
) : NavigationItem {

    override fun getName(): String {
        return psiBean.name
    }

    override fun navigate(requestFocus: Boolean) {
        navigatable?.navigate(requestFocus)
    }

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {

            override fun getPresentableText(): String {
                return name
            }

            override fun getIcon(unused: Boolean): Icon =
                if (isActive) {
                    SpringIcons.SpringBean
                } else {
                    SpringIcons.springBeanInactive
                }
        }
    }

}