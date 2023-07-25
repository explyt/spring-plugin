package com.esprito.jpa.ql.parser;

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
    private static final Map<IElementType, Key<Boolean>> tokenKeys = FactoryMap.createMap(o -> new Key<>(o.getDebugName()), ConcurrentHashMap::new);

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