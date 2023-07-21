package com.esprito.jpa.ql.parser;

import com.esprito.jpa.ql.psi.JpqlTypes;
import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.psi.tree.IElementType;

public class JpqlParserUtil extends GeneratedParserUtilBase {
    public static boolean isUnitTestMode(PsiBuilder builder, int level) {
        return ApplicationManager.getApplication().isInternal()
                || ApplicationManager.getApplication().isUnitTestMode();
    }

    public static boolean isHql(PsiBuilder builder, int level) {
        return true;
    }

    private static final Key<Integer> PAREN_STACK = new Key<>("PAREN_STACK");

    public static boolean rParenRecovery(PsiBuilder builder, int level) {
        IElementType tokenType = builder.getTokenType();

        int stackDepth = PAREN_STACK.get(builder, 0);
        if(stackDepth == -1) {
            PAREN_STACK.set(builder, 0);
            return false;
        }

        if(tokenType == JpqlTypes.LPAREN) {
            PAREN_STACK.set(builder, stackDepth + 1);
        }

        if(tokenType == JpqlTypes.RPAREN) {
            PAREN_STACK.set(builder, stackDepth - 1);
        }

        return true;
    }

    private static Language getLanguage(PsiBuilder builder_) {
        PsiFile file = builder_.getUserData(FileContextUtil.CONTAINING_FILE_KEY);

        assert file != null;

        return file.getLanguage();
    }
}