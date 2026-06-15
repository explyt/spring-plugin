/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.jpa.ql.psi.impl

import com.explyt.jpa.ql.psi.JpqlNameIdentifierOwner
import com.intellij.lang.ASTNode

abstract class JpqlNameIdentifierOwnerImpl(astNode: ASTNode) : JpqlNamedElementImpl(astNode), JpqlNameIdentifierOwner