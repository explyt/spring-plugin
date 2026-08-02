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

/**
 * A Search Everywhere item for a Spring bean.
 *
 * [navigatable] is resolved lazily (and outside the EDT, by the contributor's read action) only for items that
 * actually matched the query. It stays nullable because `EditSourceUtil.getDescriptor` returns null for members
 * without a navigable source, hence [canNavigate] reports the real capability instead of silently doing nothing.
 */
class BeanNavigationItem(
    private val psiBean: PsiBean,
    private val isActive: Boolean,
    private val navigatable: Navigatable?
) : NavigationItem {

    override fun getName(): String {
        return psiBean.name
    }

    override fun canNavigate(): Boolean = navigatable?.canNavigate() ?: false

    override fun canNavigateToSource(): Boolean = navigatable?.canNavigateToSource() ?: false

    override fun navigate(requestFocus: Boolean) {
        val navigatable = navigatable ?: return
        if (!navigatable.canNavigate()) return
        navigatable.navigate(requestFocus)
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
