package com.esprito.jpa.ql.parser;

import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.resolve.FileContextUtil;

public class JpqlParserUtil extends GeneratedParserUtilBase {
    public static boolean isUnitTestMode(PsiBuilder builder, int level) {
        return ApplicationManager.getApplication().isInternal()
                || ApplicationManager.getApplication().isUnitTestMode();
    }

    public static boolean isHql(PsiBuilder builder, int level) {
        return true;
    }

    private static Language getLanguage(PsiBuilder builder_) {
        PsiFile file = builder_.getUserData(FileContextUtil.CONTAINING_FILE_KEY);

        assert file != null;

        return file.getLanguage();
    }
}