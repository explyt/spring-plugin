/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd
 * and are protected by copyright and other intellectual property laws.
 *
 * Original work: org.springframework.data.repository.query.parser.PartTree
 * Available during publication at https://github.com/spring-projects/spring-data-commons/blob/3.0.x/src/main/java/org/springframework/data/repository/query/parser/PartTree.java
 * Licensed under the Apache License, Version 2.0:
 *     Copyright © 2008-2024 Oliver Gierke, Jens Schauder,
 *     Christoph Strobl, Thomas Darimont, and other contributors.
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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.util.ArrayUtilRt;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to parse a {@link String} into a tree or {@link OrPart}s consisting of simple {@link Part} instances in turn.
 * Takes a domain class as well to validate that each of the {@link Part}s are referring to a property of the domain
 * class. The {@link PartTree} can then be used to build queries based on its API instead of parsing the method name for
 * each query execution.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 */
public class PartTree implements Iterable<PartTree.OrPart> {

    public static final String OR_OPERATOR = "Or";
    public static final String AND_OPERATOR = "And";

    private static final String KEYWORD_TEMPLATE = "(%s)(?=(\\p{Lu}|\\P{InBASIC_LATIN}))";
    public static final String QUERY_PATTERN = "find|read|get|query|search|stream";
    public static final String COUNT_PATTERN = "count";
    public static final String EXISTS_PATTERN = "exists";
    public static final String DELETE_PATTERN = "delete|remove";
    private static final Pattern PREFIX_TEMPLATE = Pattern.compile( //
            "^(" + QUERY_PATTERN + "|" + COUNT_PATTERN + "|" + EXISTS_PATTERN + "|" + DELETE_PATTERN + ")((\\p{Lu}.*?))??By");

    private final Subject subject;
    private final Predicate predicate;
    private final String mySource;

    public PartTree(String source, PsiClass domainClass) {
        mySource = source;

        Matcher matcher = PREFIX_TEMPLATE.matcher(source);
        if (!matcher.find()) {
            this.subject = new Subject(null);
            this.predicate = new Predicate(source, domainClass, 0);
        } else {
            this.subject = new Subject(matcher.group(0));
            int offset = matcher.group().length();
            this.predicate = new Predicate(source.substring(offset), domainClass, offset);
        }
    }

    public String getSource() {
        return mySource;
    }

    @Override
    public Iterator<OrPart> iterator() {
        return predicate.iterator();
    }

    public Sort getSort() {

        OrderBySource orderBySource = getOrderBySource();
        return orderBySource == null ? null : orderBySource.toSort();
    }

    public OrderBySource getOrderBySource() {
        return predicate.getOrderBySource();
    }

    public List<Part> getParts() {

        List<Part> result = new ArrayList<>();
        for (OrPart orPart : this) {
            for (Part part : orPart) {
                result.add(part);
            }
        }
        return result;
    }

    public Iterable<Part> getParts(Part.Type type) {
        List<Part> result = new ArrayList<>();

        for (Part part : getParts()) {
            if (part.getType().equals(type)) {
                result.add(part);
            }
        }

        return result;
    }

    @Override
    public String toString() {
        OrderBySource orderBySource = getOrderBySource();
        return String.format("%s%s", StringUtil.join(predicate.nodes, " or "),
                orderBySource == null ? "" : " " + orderBySource);
    }

    private String[] split(String text, String keyword) {
        if (text.equals(keyword)) return new String[]{"", ""};
        String format = String.format(KEYWORD_TEMPLATE, keyword);


        if (text.endsWith(keyword)) {
            List<String> splitted = StringUtil.split(text, keyword);
            splitted.add("");
            return ArrayUtilRt.toStringArray(splitted);
        }

        Pattern pattern = Pattern.compile(format);
        return pattern.split(text);
    }

    public class OrPart implements Iterable<Part> {

        private final List<Part> children = new ArrayList<>();
        private final String mySource;

        OrPart(String source, PsiClass domainClass, boolean alwaysIgnoreCase, int offset) {
            mySource = source;
            String[] split = split(source, AND_OPERATOR);
            int currentOffset = 0;
            for (String part : split) {
                currentOffset = StringUtil.indexOf(source, part, currentOffset);
                children.add(new Part(part, domainClass, alwaysIgnoreCase, offset + currentOffset));
                currentOffset += part.length() + AND_OPERATOR.length();
            }
        }

        @Override
        public Iterator<Part> iterator() {
            return children.iterator();
        }

        @Override
        public String toString() {
            return "OR ('" + mySource + "')";
        }
    }

    public class Subject {

        private static final String DISTINCT = "Distinct";
        private static final Pattern COUNT_BY_TEMPLATE = Pattern.compile("^count(\\p{Lu}.*?)??By");
        private static final Pattern EXISTS_BY_TEMPLATE = Pattern.compile("^(" + EXISTS_PATTERN + ")(\\p{Lu}.*?)??By");
        private static final Pattern DELETE_BY_TEMPLATE = Pattern.compile("^(" + DELETE_PATTERN + ")(\\p{Lu}.*?)??By");
        private static final String LIMITING_QUERY_PATTERN = "(First|Top)(\\d*)?";
        private static final Pattern LIMITED_QUERY_TEMPLATE = Pattern
                .compile("^(" + QUERY_PATTERN + ")(" + DISTINCT + ")?" + LIMITING_QUERY_PATTERN + "(\\p{Lu}.*?)??By");

        private final String myExpression;
        private final boolean exists;
        private final boolean count;
        private final boolean delete;

        public Subject(String subject) {
            this.myExpression = subject;
            this.count = matches(subject, COUNT_BY_TEMPLATE);
            this.delete = matches(subject, DELETE_BY_TEMPLATE);
            this.exists = matches(subject, EXISTS_BY_TEMPLATE);
        }

        public Boolean isDelete() {
            return delete;
        }

        public boolean isCountProjection() {
            return count;
        }

        public boolean isExistsProjection() {
            return exists;
        }

        private boolean matches(String subject, Pattern pattern) {
            return subject != null && pattern.matcher(subject).find();
        }

        public String getExpression() {
            return myExpression;
        }

        @Override
        public String toString() {
            return "SUBJECT ('" + myExpression + ')';
        }
    }

    private class Predicate {

        private final Pattern ALL_IGNORE_CASE = Pattern.compile("AllIgnor(ing|e)Case");
        private static final String ORDER_BY = "OrderBy";

        private final List<OrPart> nodes = new ArrayList<>();
        private final OrderBySource orderBySource;
        private final int offset;
        private boolean alwaysIgnoreCase;

        Predicate(String predicate, PsiClass domainClass, int offset) {
            this.offset = offset;
            String sourceString = detectAndSetAllIgnoreCase(predicate);
            String[] parts = split(sourceString, ORDER_BY);

            buildTree(parts[0], domainClass);

            int sortIndex = offset + StringUtil.indexOf(sourceString, ORDER_BY) + ORDER_BY.length();
            this.orderBySource = parts.length == 2 ? new OrderBySource(parts[1], domainClass, sortIndex) : null;
        }

        private String detectAndSetAllIgnoreCase(String predicate) {

            Matcher matcher = ALL_IGNORE_CASE.matcher(predicate);

            if (matcher.find()) {
                alwaysIgnoreCase = true;
                predicate = predicate.substring(0, matcher.start()) + predicate.substring(matcher.end());
            }

            return predicate;
        }

        private void buildTree(String source, PsiClass domainClass) {
            String[] split = split(source, OR_OPERATOR);
            int currentOffset = 0;
            for (String part : split) {
                currentOffset = StringUtil.indexOf(source, part, currentOffset);
                nodes.add(new OrPart(part, domainClass, alwaysIgnoreCase, offset + currentOffset));
                currentOffset += part.length() + OR_OPERATOR.length();
            }
        }

        public Iterator<OrPart> iterator() {
            return nodes.iterator();
        }

        public OrderBySource getOrderBySource() {
            return orderBySource;
        }
    }

    public Subject getSubject() {
        return subject;
    }

    public Predicate getPredicate() {
        return predicate;
    }
}
