/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.sql.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Key;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.FactoryMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SqlParserUtil extends GeneratedParserUtilBase {
    private static final Map<IElementType, Key<Boolean>> tokenKeys = FactoryMap.createMap(o -> new Key<>(o.toString()), ConcurrentHashMap::new);

    public static boolean isUnitTestMode(PsiBuilder builder, int level) {
        return ApplicationManager.getApplication().isInternal()
                || ApplicationManager.getApplication().isUnitTestMode();
    }
}