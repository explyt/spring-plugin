/*
 * Copyright 2008-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.query.parser;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.repository.query.parser.domain.OrderBySource;
import org.springframework.data.repository.query.parser.domain.Sort;

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

    /*
     * We look for a pattern of: keyword followed by
     *
     *  an upper-case letter that has a lower-case variant \p{Lu}
     * OR
     *  any other letter NOT in the BASIC_LATIN Uni-code Block \\P{InBASIC_LATIN} (like Chinese, Korean, Japanese, etc.).
     *
     * @see <a href="https://www.regular-expressions.info/unicode.html">https://www.regular-expressions.info/unicode.html</a>
     * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#ubc">Pattern</a>
     */
    private static final String KEYWORD_TEMPLATE = "(%s)(?=(\\p{Lu}|\\P{InBASIC_LATIN}))";
    public static final String QUERY_PATTERN = "find|read|get|query|search|stream";
    public static final String COUNT_PATTERN = "count";
    public static final String EXISTS_PATTERN = "exists";
    public static final String DELETE_PATTERN = "delete|remove";
    private static final Pattern PREFIX_TEMPLATE = Pattern.compile( //
            "^(" + QUERY_PATTERN + "|" + COUNT_PATTERN + "|" + EXISTS_PATTERN + "|" + DELETE_PATTERN + ")((\\p{Lu}.*?))??By");

    /**
     * The subject, for example "findDistinctUserByNameOrderByAge" would have the subject "DistinctUser".
     */
    private final Subject subject;

    /**
     * The subject, for example "findDistinctUserByNameOrderByAge" would have the predicate "NameOrderByAge".
     */
    private final Predicate predicate;
    private final String mySource;

    /**
     * Creates a new {@link PartTree} by parsing the given {@link String}.
     *
     * @param source      the {@link String} to parse
     * @param domainClass the domain class to check individual parts against to ensure they refer to a property of the
     *                    class
     */
    public PartTree(@NotNull String source, @NotNull PsiClass domainClass) {
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

    /*
     * (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<OrPart> iterator() {
        return predicate.iterator();
    }

    /**
     * Returns the {@link Sort} specification parsed from the source or <tt>null</tt>.
     *
     * @return the sort
     */
    public Sort getSort() {

        OrderBySource orderBySource = getOrderBySource();
        return orderBySource == null ? null : orderBySource.toSort();
    }

    @Nullable
    public OrderBySource getOrderBySource() {
        return predicate.getOrderBySource();
    }

    /**
     * Returns whether we indicate distinct lookup of entities.
     *
     * @return {@literal true} if distinct
     */
    public boolean isDistinct() {
        return subject.isDistinct();
    }

    /**
     * Returns whether a count projection shall be applied.
     *
     * @return
     */
    public Boolean isCountProjection() {
        return subject.isCountProjection();
    }

    public Boolean isExistsProjection() {
        return subject.isExistsProjection();
    }

    /**
     * return true if the created {@link PartTree} is meant to be used for delete operation.
     *
     * @return
     * @since 1.8
     */
    public Boolean isDelete() {
        return subject.isDelete();
    }

    /**
     * Return {@literal true} if the create {@link PartTree} is meant to be used for a query with limited maximal results.
     *
     * @return
     * @since 1.9
     */
    public boolean isLimiting() {
        return getMaxResults() != null;
    }

    /**
     * Return the number of maximal results to return or {@literal null} if not restricted.
     *
     * @return
     * @since 1.9
     */
    public Integer getMaxResults() {
        return subject.getMaxResults();
    }

    /**
     * Returns an {@link Iterable} of all parts contained in the {@link PartTree}.
     *
     * @return the iterable {@link Part}s
     */
    public List<Part> getParts() {

        List<Part> result = new ArrayList<>();
        for (OrPart orPart : this) {
            for (Part part : orPart) {
                result.add(part);
            }
        }
        return result;
    }

    /**
     * Returns all {@link Part}s of the {@link PartTree} of the given {@link Type}.
     *
     * @param type
     * @return
     */
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

    /**
     * Splits the given text at the given keywords. Expects camel-case style to only match concrete keywords and not
     * derivatives of it.
     *
     * @param text    the text to split
     * @param keyword the keyword to split around
     * @return an array of split items
     */
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

    /**
     * A part of the parsed source that results from splitting up the resource around {@literal Or} keywords. Consists of
     * {@link Part}s that have to be concatenated by {@literal And}.
     */
    public class OrPart implements Iterable<Part> {

        private final List<Part> children = new ArrayList<>();
        private final String mySource;

        /**
         * Creates a new {@link OrPart}.
         *
         * @param source           the source to split up into {@literal And} parts in turn.
         * @param domainClass      the domain class to check the resulting {@link Part}s against.
         * @param alwaysIgnoreCase if always ignoring case
         */
        OrPart(@NotNull String source, PsiClass domainClass, boolean alwaysIgnoreCase, int offset) {
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

    /**
     * Represents the subject part of the query. E.g. {@code findDistinctUserByNameOrderByAge} would have the subject
     * {@code DistinctUser}.
     *
     * @author Phil Webb
     * @author Oliver Gierke
     * @author Christoph Strobl
     * @author Thomas Darimont
     */
    public class Subject {

        private static final String DISTINCT = "Distinct";
        private static final Pattern COUNT_BY_TEMPLATE = Pattern.compile("^count(\\p{Lu}.*?)??By");
        private static final Pattern EXISTS_BY_TEMPLATE = Pattern.compile("^(" + EXISTS_PATTERN + ")(\\p{Lu}.*?)??By");
        private static final Pattern DELETE_BY_TEMPLATE = Pattern.compile("^(" + DELETE_PATTERN + ")(\\p{Lu}.*?)??By");
        private static final String LIMITING_QUERY_PATTERN = "(First|Top)(\\d*)?";
        private static final Pattern LIMITED_QUERY_TEMPLATE = Pattern
                .compile("^(" + QUERY_PATTERN + ")(" + DISTINCT + ")?" + LIMITING_QUERY_PATTERN + "(\\p{Lu}.*?)??By");

        private final String myExpression;
        private final boolean distinct;
        private final boolean exists;
        private final boolean count;
        private final boolean delete;
        private final Integer maxResults;

        public Subject(String subject) {
            this.myExpression = subject;
            this.distinct = subject == null ? false : subject.contains(DISTINCT);
            this.count = matches(subject, COUNT_BY_TEMPLATE);
            this.delete = matches(subject, DELETE_BY_TEMPLATE);
            this.exists = matches(subject, EXISTS_BY_TEMPLATE);
            this.maxResults = returnMaxResultsIfFirstKSubjectOrNull(subject);
        }

        /**
         * @param subject
         * @return
         * @since 1.9
         */
        private Integer returnMaxResultsIfFirstKSubjectOrNull(String subject) {

            if (subject == null) {
                return null;
            }

            Matcher grp = LIMITED_QUERY_TEMPLATE.matcher(subject);

            if (!grp.find()) {
                return null;
            }

            return StringUtil.isNotEmpty(grp.group(4)) ? Integer.parseInt(grp.group(4)) : 1;
        }

        /**
         * Returns {@literal true} if {@link Subject} matches {@link #DELETE_BY_TEMPLATE}.
         *
         * @return
         * @since 1.8
         */
        public Boolean isDelete() {
            return delete;
        }

        public boolean isCountProjection() {
            return count;
        }

        public boolean isExistsProjection() {
            return exists;
        }

        public boolean isDistinct() {
            return distinct;
        }

        public Integer getMaxResults() {
            return maxResults;
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

    /**
     * Represents the predicate part of the query.
     *
     * @author Oliver Gierke
     * @author Phil Webb
     */
    private class Predicate {

        private final Pattern ALL_IGNORE_CASE = Pattern.compile("AllIgnor(ing|e)Case");
        private static final String ORDER_BY = "OrderBy";

        private final List<OrPart> nodes = new ArrayList<>();
        private final OrderBySource orderBySource;
        private final int offset;
        private boolean alwaysIgnoreCase;

        Predicate(@NotNull String predicate, @NotNull PsiClass domainClass, int offset) {
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

        private void buildTree(@NotNull String source, @NotNull PsiClass domainClass) {
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
