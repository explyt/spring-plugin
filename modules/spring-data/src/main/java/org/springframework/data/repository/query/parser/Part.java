/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd
 * and are protected by copyright and other intellectual property laws.
 *
 * Original work: org.springframework.data.repository.query.parser.Part
 * Available during publication at https://github.com/spring-projects/spring-data-commons/blob/3.0.x/src/main/java/org/springframework/data/repository/query/parser/Part.java
 * Licensed under the Apache License, Version 2.0:
 *     Copyright © 2008-2024 Oliver Gierke, Martin Baumgartner,
 *     Jens Schauder, Thomas Darimont, Michael Cramer, and other contributors.
 *
 * Modifications to the original work have been made by Explyt Ltd.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"),
 * if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code,
 * you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code
 * and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at:
 *
 *     https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights
 * and may result in legal action.
 *
 * Modifications Made:
 *     **Modified:**
 *         - Updated for local usages.
 *         - Optimize for usage IDEA code model - com.intellij.psi.PsiClass instead of java.lang.Class.
 *     **Removed:**
 *         - Removed unused code.
 *
 * NOTICE:
 *     This file includes code from an original work licensed under the Apache License 2.0.
 *     The original license and copyright notices are retained.
 *     This entire file, including modifications to the original work, is licensed under
 *     the Explyt Source License. To use this file, you must agree to the terms of the
 *     Explyt Source License.
 *
 * See the Apache License, Version 2.0, for the specific language governing permissions
 * and limitations under the original work's license.
 */
package org.springframework.data.repository.query.parser;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiSubstitutor;
import org.springframework.data.mapping.PropertyPath;

import java.beans.Introspector;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A single part of a method name that has to be transformed into a query part. The actual transformation is defined by
 * a {@link Type} that is determined from inspecting the given part. The query part can then be looked up via
 * {@link #getProperty()}.
 *
 * @author Oliver Gierke
 * @author Martin Baumgartner
 * @author Jens Schauder
 */
public class Part {

    private static final Pattern IGNORE_CASE = Pattern.compile("Ignor(ing|e)Case");

    private final PropertyPath propertyPath;
    private final Type type;

    private IgnoreCaseType ignoreCase = IgnoreCaseType.NEVER;
    private final String mySource;
    private final int offset;


    public Part(String source, PsiClass clazz, boolean alwaysIgnoreCase, int offset) {
        mySource = source;
        this.offset = offset;
        String partToUse = detectAndSetIgnoreCase(source);
        if (alwaysIgnoreCase && ignoreCase != IgnoreCaseType.ALWAYS) {
            this.ignoreCase = IgnoreCaseType.WHEN_POSSIBLE;
        }
        this.type = Type.fromProperty(partToUse);
        this.propertyPath = PropertyPath.from(type.extractProperty(partToUse), JavaPsiFacade
                .getElementFactory(clazz.getProject()).createType(clazz, PsiSubstitutor.EMPTY));
    }

    public int getOffset() {
        return offset;
    }

    public int getEndOffset() {
        return offset + mySource.length();
    }

    private String detectAndSetIgnoreCase(String part) {
        Matcher matcher = IGNORE_CASE.matcher(part);
        String result = part;

        if (matcher.find()) {
            ignoreCase = IgnoreCaseType.ALWAYS;
            result = part.substring(0, matcher.start()) + part.substring(matcher.end());
        }

        return result;
    }

    public int getNumberOfArguments() {
        return type.getNumberOfArguments();
    }

    public PropertyPath getProperty() {
        return propertyPath;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }

        Part that = (Part) obj;
        return this.propertyPath.equals(that.propertyPath) && this.type.equals(that.type);
    }

    @Override
    public int hashCode() {
        int result = 37;
        result += 17 * propertyPath.hashCode();
        result += 17 * type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PART ('" + mySource + "'," + type.name() + ")";
    }

    public String getSource() {
        return mySource;
    }

    public enum Type {

        BETWEEN(2, "IsBetween", "Between"),
        IS_NOT_NULL(0, "IsNotNull", "NotNull"),
        IS_NULL(0, "IsNull", "Null"),
        LESS_THAN("IsLessThan", "LessThan"),
        LESS_THAN_EQUAL("IsLessThanEqual", "LessThanEqual"),
        GREATER_THAN("IsGreaterThan", "GreaterThan"),
        GREATER_THAN_EQUAL("IsGreaterThanEqual", "GreaterThanEqual"),
        BEFORE("IsBefore", "Before"),
        AFTER("IsAfter", "After"),
        NOT_LIKE("IsNotLike", "NotLike"),
        LIKE("IsLike", "Like"),
        STARTING_WITH("IsStartingWith", "StartingWith", "StartsWith"),
        ENDING_WITH("IsEndingWith", "EndingWith", "EndsWith"),
        IS_NOT_EMPTY(0, "IsNotEmpty", "NotEmpty"),
        IS_EMPTY(0, "IsEmpty", "Empty"),
        NOT_CONTAINING("IsNotContaining", "NotContaining", "NotContains"),
        CONTAINING("IsContaining", "Containing", "Contains"),
        NOT_IN("IsNotIn", "NotIn"),
        IN("IsIn", "In"),
        NEAR("IsNear", "Near"),
        WITHIN("IsWithin", "Within"),
        REGEX("MatchesRegex", "Matches", "Regex"),
        EXISTS(0, "Exists"),
        TRUE(0, "IsTrue", "True"),
        FALSE(0, "IsFalse", "False"),
        NEGATING_SIMPLE_PROPERTY("IsNot", "Not"),
        SIMPLE_PROPERTY("Is", "Equals");

        private static final List<Type> ALL = Arrays.asList(IS_NOT_NULL, IS_NULL, BETWEEN, LESS_THAN, LESS_THAN_EQUAL,
                GREATER_THAN, GREATER_THAN_EQUAL, BEFORE, AFTER, NOT_LIKE, LIKE, STARTING_WITH, ENDING_WITH, IS_NOT_EMPTY,
                IS_EMPTY, NOT_CONTAINING, CONTAINING, NOT_IN, IN, NEAR, WITHIN, REGEX, EXISTS, TRUE, FALSE,
                NEGATING_SIMPLE_PROPERTY, SIMPLE_PROPERTY);

        public static final Collection<String> ALL_KEYWORDS;

        static {
            List<String> allKeywords = new ArrayList<>();
            for (Type type : ALL) {
                allKeywords.addAll(type.keywords);
            }
            ALL_KEYWORDS = Collections.unmodifiableList(allKeywords);
        }

        private final List<String> keywords;
        private final int numberOfArguments;

        Type(int numberOfArguments, String... keywords) {

            this.numberOfArguments = numberOfArguments;
            this.keywords = Arrays.asList(keywords);
        }

        Type(String... keywords) {
            this(1, keywords);
        }

        public static Type fromProperty(String rawProperty) {
            for (Type type : ALL) {
                if (type.supports(rawProperty)) {
                    return type;
                }
            }

            return SIMPLE_PROPERTY;
        }

        public Collection<String> getKeywords() {
            return Collections.unmodifiableList(keywords);
        }

        private boolean supports(String property) {

            for (String keyword : keywords) {
                if (property.endsWith(keyword)) {
                    return true;
                }
            }

            return false;
        }

        public int getNumberOfArguments() {
            return numberOfArguments;
        }

        public String extractProperty(String part) {

            String candidate = Introspector.decapitalize(part);

            for (String keyword : keywords) {
                if (candidate.endsWith(keyword)) {
                    return candidate.substring(0, candidate.length() - keyword.length());
                }
            }

            return candidate;
        }

        @Override
        public String toString() {
            return String.format("%s (%s): %s", name(), getNumberOfArguments(), getKeywords());
        }
    }

    public enum IgnoreCaseType {
        NEVER, ALWAYS, WHEN_POSSIBLE
    }
}