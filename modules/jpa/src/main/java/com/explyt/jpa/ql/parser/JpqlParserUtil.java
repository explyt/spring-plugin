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

package com.explyt.jpa.ql.parser;

import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.FactoryMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JpqlParserUtil extends GeneratedParserUtilBase {
    private static final Map<IElementType, Key<Boolean>> tokenKeys = FactoryMap.createMap(o -> new Key<>(o.toString()), ConcurrentHashMap::new);

    public static boolean isUnitTestMode(PsiBuilder builder, int level) {
        return ApplicationManager.getApplication().isInternal()
                || ApplicationManager.getApplication().isUnitTestMode();
    }

    public static boolean isHql(PsiBuilder builder, int level) {
        return true;
    }

    private static Language getLanguage(PsiBuilder builder) {
        PsiFile file = builder.getUserData(FileContextUtil.CONTAINING_FILE_KEY);

        assert file != null;

        return file.getLanguage();
    }

    public static boolean facedToken(PsiBuilder builder, int level, IElementType tokenType) {
        tokenKeys.get(tokenType).set(builder, true);
        return true;
    }

    public static boolean notFaced(PsiBuilder builder, int level, IElementType tokenType) {
        return !tokenKeys.get(tokenType).get(builder, false);
    }

    public static boolean resetToken(PsiBuilder builder, int level, IElementType tokenType) {
        tokenKeys.get(tokenType).set(builder, false);
        return true;
    }
}