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
package org.springframework.data.repository.query.parser.domain;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

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

    private static final long serialVersionUID = 5737186511678863905L;
    public static final Direction DEFAULT_DIRECTION = Direction.ASC;

    private final List<Order> orders;

    /**
     * Creates a new {@link Sort} instance using the given {@link Order}s.
     *
     * @param orders must not be {@literal null}.
     */
    public Sort(Order... orders) {
        this(Arrays.asList(orders));
    }

    /**
     * Creates a new {@link Sort} instance.
     *
     * @param orders must not be {@literal null} or contain {@literal null}.
     */
    public Sort(@NotNull List<Order> orders) {
        this.orders = orders;
    }

    /**
     * Creates a new {@link Sort} instance. Order defaults to {@link Direction#ASC}.
     *
     * @param properties must not be {@literal null} or contain {@literal null} or empty strings
     */
    public Sort(String... properties) {
        this(DEFAULT_DIRECTION, properties);
    }

    /**
     * Creates a new {@link Sort} instance.
     *
     * @param direction  defaults to {@linke Sort#DEFAULT_DIRECTION} (for {@literal null} cases, too)
     * @param properties must not be {@literal null}, empty or contain {@literal null} or empty strings.
     */
    public Sort(Direction direction, String... properties) {
        this(direction, properties == null ? new ArrayList<>() : Arrays.asList(properties));
    }

    /**
     * Creates a new {@link Sort} instance.
     *
     * @param direction  defaults to {@linke Sort#DEFAULT_DIRECTION} (for {@literal null} cases, too)
     * @param properties must not be {@literal null} or contain {@literal null} or empty strings.
     */
    public Sort(Direction direction, List<String> properties) {
        this.orders = new ArrayList<>(properties.size());

        for (String property : properties) {
            this.orders.add(new Order(direction, property, property, 0));
        }
    }

    /**
     * Returns a new {@link Sort} consisting of the {@link Order}s of the current {@link Sort} combined with the given
     * ones.
     *
     * @param sort can be {@literal null}.
     * @return
     */
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

    /**
     * Returns the order registered for the given property.
     *
     * @param property
     * @return
     */
    public Order getOrderFor(String property) {

        for (Order order : this) {
            if (order.getProperty().equals(property)) {
                return order;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Order> iterator() {
        return this.orders.iterator();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
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

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {

        int result = 17;
        result = 31 * result + orders.hashCode();
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return StringUtil.join(orders, ",");
    }

    /**
     * Enumeration for sort directions.
     *
     * @author Oliver Gierke
     */
    public enum Direction {

        ASC, DESC;

        /**
         * Returns the {@link Direction} enum for the given {@link String} value.
         *
         * @param value
         * @return
         * @throws IllegalArgumentException in case the given value cannot be parsed into an enum value.
         */
        public static Direction fromString(String value) {
            return valueOf(StringUtil.toUpperCase(value));
        }

        /**
         * Returns the {@link Direction} enum for the given {@link String} or null if it cannot be parsed into an enum
         * value.
         *
         * @param value
         * @return
         */
        public static Direction fromStringOrNull(String value) {
            return fromString(value);
        }
    }

    /**
     * Enumeration for null handling hints that can be used in {@link Order} expressions.
     *
     * @author Thomas Darimont
     * @since 1.8
     */
    public enum NullHandling {

        /**
         * Lets the data store decide what to do with nulls.
         */
        NATIVE,

        /**
         * A hint to the used data store to order entries with null values before non null entries.
         */
        NULLS_FIRST,

        /**
         * A hint to the used data store to order entries with null values after non null entries.
         */
        NULLS_LAST
    }

    /**
     * PropertyPath implements the pairing of an {@link Direction} and a property. It is used to provide input for
     * {@link Sort}
     *
     * @author Oliver Gierke
     * @author Kevin Raymond
     */
    public static class Order implements Serializable {

        private static final long serialVersionUID = 1522511010900108987L;
        private static final boolean DEFAULT_IGNORE_CASE = false;

        private final Direction direction;
        private final String property;
        private final boolean ignoreCase;
        private final NullHandling nullHandling;
        private PropertyPath myPath;
        private String myPropertySource;
        private final String mySortExpression;
        private final int offset;

        /**
         * Creates a new {@link Order} instance. if order is {@literal null} then order defaults to
         * {@link Sort#DEFAULT_DIRECTION}
         *
         * @param direction can be {@literal null}, will default to {@link Sort#DEFAULT_DIRECTION}
         * @param property  must not be {@literal null} or empty.
         */
        public Order(Direction direction, String property, String sortExpression, int offset) {
            this(direction, property, DEFAULT_IGNORE_CASE, null, sortExpression, offset);
        }

        /**
         * Creates a new {@link Order} instance. if order is {@literal null} then order defaults to
         * {@link Sort#DEFAULT_DIRECTION}
         *
         * @param direction        can be {@literal null}, will default to {@link Sort#DEFAULT_DIRECTION}
         * @param property         must not be {@literal null} or empty.
         * @param nullHandlingHint can be {@literal null}, will default to {@link NullHandling#NATIVE}.
         */
        public Order(Direction direction, String property, NullHandling nullHandlingHint, int offset) {
            this(direction, property, DEFAULT_IGNORE_CASE, nullHandlingHint, property, offset);
        }

        /**
         * Creates a new {@link Order} instance. if order is {@literal null} then order defaults to
         * {@link Sort#DEFAULT_DIRECTION}
         *
         * @param direction    can be {@literal null}, will default to {@link Sort#DEFAULT_DIRECTION}
         * @param property     must not be {@literal null} or empty.
         * @param ignoreCase   true if sorting should be case insensitive. false if sorting should be case sensitive.
         * @param nullHandling can be {@literal null}, will default to {@link NullHandling#NATIVE}.
         * @since 1.7
         */
        private Order(Direction direction, @NotNull String property, boolean ignoreCase,
                      NullHandling nullHandling, String sortExpression, int offset) {
            if (StringUtil.isEmptyOrSpaces(property)) {
                // throw new IllegalArgumentException("Property must not null or empty!");
            }
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

        public String getSortExpression() {
            return mySortExpression;
        }

        public PropertyPath getPropertyPath() {
            return myPath;
        }

        /**
         * Returns the order the property shall be sorted for.
         *
         * @return
         */
        public Direction getDirection() {
            return direction;
        }

        /**
         * Returns the property to order for.
         *
         * @return
         */
        public String getProperty() {
            return property;
        }

        public String getPropertySourceExpression() {
            return myPropertySource;
        }

        /**
         * Returns whether sorting for this property shall be ascending.
         *
         * @return
         */
        public boolean isAscending() {
            return this.direction.equals(Direction.ASC);
        }

        /**
         * Returns whether or not the sort will be case sensitive.
         *
         * @return
         */
        public boolean isIgnoreCase() {
            return ignoreCase;
        }

        /**
         * Returns a new {@link Order} with the given {@link Order}.
         *
         * @param order
         * @return
         */
        public Order with(Direction order) {
            return new Order(order, this.property, nullHandling, offset);
        }

        /**
         * Returns a new {@link Sort} instance for the given properties.
         *
         * @param properties
         * @return
         */
        public Sort withProperties(String... properties) {
            return new Sort(this.direction, properties);
        }

        /**
         * Returns a new {@link Order} with case insensitive sorting enabled.
         *
         * @return
         */
        public Order ignoreCase() {
            return new Order(direction, property, true, nullHandling, property, offset);
        }

        /**
         * Returns a {@link Order} with the given {@link NullHandling}.
         *
         * @param nullHandling can be {@literal null}.
         * @return
         * @since 1.8
         */
        public Order with(NullHandling nullHandling) {
            return new Order(direction, this.property, ignoreCase, nullHandling, this.property, offset);
        }

        /**
         * Returns a {@link Order} with {@link NullHandling#NULLS_FIRST} as null handling hint.
         *
         * @return
         * @since 1.8
         */
        public Order nullsFirst() {
            return with(NullHandling.NULLS_FIRST);
        }

        /**
         * Returns a {@link Order} with {@link NullHandling#NULLS_LAST} as null handling hint.
         *
         * @return
         * @since 1.7
         */
        public Order nullsLast() {
            return with(NullHandling.NULLS_LAST);
        }

        /**
         * Returns a {@link Order} with {@link NullHandling#NATIVE} as null handling hint.
         *
         * @return
         * @since 1.7
         */
        public Order nullsNative() {
            return with(NullHandling.NATIVE);
        }

        /**
         * Returns the used {@link NullHandling} hint, which can but may not be respected by the used datastore.
         *
         * @return
         * @since 1.7
         */
        public NullHandling getNullHandling() {
            return nullHandling;
        }

        public int getOffset() {
            return offset;
        }

        public int getEndOffset() {
            return offset + (property == null ? 0 : property.length());
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {

            int result = 17;

            result = 31 * result + direction.hashCode();
            result = 31 * result + property.hashCode();
            result = 31 * result + (ignoreCase ? 1 : 0);
            result = 31 * result + nullHandling.hashCode();

            return result;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
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

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            String result = String.format("%s: %s", StringUtil.isEmptyOrSpaces(property) ? "<empty>" : property, direction);
            if (!NullHandling.NATIVE.equals(nullHandling)) {
                result += ", " + nullHandling;
            }
            if (ignoreCase) {
                result += ", ignoring case";
            }
            return result;
        }
    }
}
