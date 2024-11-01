/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd
 * and are protected by copyright and other intellectual property laws.
 *
 * Original work: org.springframework.data.domain.Sort
 * Available during publication at https://github.com/spring-projects/spring-data-commons/blob/3.0.x/src/main/java/org/springframework/data/domain/Sort.java
 * Licensed under the Apache License, Version 2.0:
 *     Copyright © 2008-2024 Oliver Gierke, Thomas Darimont, Mark Paluch,
 *     Johannes Englmeier, Kevin Raymond, and other contributors.
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

package org.springframework.data.domain;

import com.intellij.openapi.util.text.StringUtil;
import org.springframework.data.mapping.PropertyPath;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Sort option for queries. You have to provide at least a list of properties to sort for that must not include
 * {@literal null} or empty strings. The direction defaults to {@link Sort#DEFAULT_DIRECTION}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class Sort implements Iterable<Sort.Order>, Serializable {

    @Serial
    private static final long serialVersionUID = 7451234671160861245L;
    public static final Direction DEFAULT_DIRECTION = Direction.ASC;

    private final List<Order> orders;

    public Sort(Order... orders) {
        this(Arrays.asList(orders));
    }

    public Sort(List<Order> orders) {
        this.orders = orders;
    }

    public Sort(String... properties) {
        this(DEFAULT_DIRECTION, properties);
    }

    public Sort(Direction direction, String... properties) {
        this(direction, properties == null ? new ArrayList<>() : Arrays.asList(properties));
    }

    public Sort(Direction direction, List<String> properties) {
        this.orders = new ArrayList<>(properties.size());

        for (String property : properties) {
            this.orders.add(new Order(direction, property, property, 0));
        }
    }

    public Sort and(Sort sort) {

        if (sort == null) {
            return this;
        }
        ArrayList<Order> these = new ArrayList<>(this.orders);

        for (Order order : sort) {
            these.add(order);
        }
        return new Sort(these);
    }

    @Override
    public Iterator<Order> iterator() {
        return this.orders.iterator();
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Sort)) {
            return false;
        }
        Sort that = (Sort) obj;

        return this.orders.equals(that.orders);
    }

    @Override
    public int hashCode() {

        int result = 17;
        result = 31 * result + orders.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return StringUtil.join(orders, ",");
    }

    public enum Direction {

        ASC, DESC;

        public static Direction fromString(String value) {
            return valueOf(StringUtil.toUpperCase(value));
        }

        public static Direction fromStringOrNull(String value) {
            return fromString(value);
        }
    }


    public enum NullHandling {
        NATIVE, NULLS_FIRST, NULLS_LAST
    }

    public static class Order implements Serializable {

        @Serial
        private static final long serialVersionUID = 8547578468957659854L;
        private static final boolean DEFAULT_IGNORE_CASE = false;
        public static final String EMPTY = "<empty>";

        private final Direction direction;
        private final String property;
        private final boolean ignoreCase;
        private final NullHandling nullHandling;
        private PropertyPath myPath;
        private String myPropertySource;
        private final String mySortExpression;
        private final int offset;

        public Order(Direction direction, String property, String sortExpression, int offset) {
            this(direction, property, DEFAULT_IGNORE_CASE, null, sortExpression, offset);
        }

        public Order(Direction direction, String property, NullHandling nullHandlingHint, int offset) {
            this(direction, property, DEFAULT_IGNORE_CASE, nullHandlingHint, property, offset);
        }

        private Order(Direction direction, String property, boolean ignoreCase,
                      NullHandling nullHandling, String sortExpression, int offset) {
            this.mySortExpression = sortExpression;
            this.direction = direction == null ? DEFAULT_DIRECTION : direction;
            this.property = property;
            this.ignoreCase = ignoreCase;
            this.nullHandling = nullHandling == null ? NullHandling.NATIVE : nullHandling;
            this.offset = offset;
        }

        public Order(Direction direction, PropertyPath path, String propertySource, String sortExpression, int offset) {
            this(direction, path.toDotPath(), sortExpression, offset);
            myPath = path;
            myPropertySource = propertySource;
        }


        public PropertyPath getPropertyPath() {
            return myPath;
        }

        public Direction getDirection() {
            return direction;
        }

        public String getProperty() {
            return property;
        }

        public boolean isAscending() {
            return this.direction.equals(Direction.ASC);
        }

        public boolean isIgnoreCase() {
            return ignoreCase;
        }

        public Order with(Direction order) {
            return new Order(order, this.property, nullHandling, offset);
        }

        public Order ignoreCase() {
            return new Order(direction, property, true, nullHandling, property, offset);
        }

        public Order with(NullHandling nullHandling) {
            return new Order(direction, this.property, ignoreCase, nullHandling, this.property, offset);
        }

        public Order nullsFirst() {
            return with(NullHandling.NULLS_FIRST);
        }

        public Order nullsLast() {
            return with(NullHandling.NULLS_LAST);
        }

        public int getOffset() {
            return offset;
        }

        public int getEndOffset() {
            return offset + (property == null ? 0 : property.length());
        }

        @Override
        public int hashCode() {

            int result = 17;

            result = 31 * result + direction.hashCode();
            result = 31 * result + property.hashCode();
            result = 31 * result + (ignoreCase ? 1 : 0);
            result = 31 * result + nullHandling.hashCode();

            return result;
        }

        @Override
        public boolean equals(Object obj) {

            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Order)) {
                return false;
            }
            Order that = (Order) obj;

            return this.direction.equals(that.direction) && this.property.equals(that.property)
                    && this.ignoreCase == that.ignoreCase && this.nullHandling.equals(that.nullHandling);
        }

        @Override
        public String toString() {
            String result = String.format("%s: %s", StringUtil.isEmptyOrSpaces(property) ? EMPTY : property, direction);
            if (!NullHandling.NATIVE.equals(nullHandling)) {
                result += ", " + nullHandling;
            }
            if (ignoreCase) {
                String ignore = ", ignoring case";
                result += ignore;
            }
            return result;
        }
    }
}
