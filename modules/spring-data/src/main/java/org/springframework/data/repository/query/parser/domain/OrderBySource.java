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
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple helper class to create a {@link Sort} instance from a method name end. It expects the last part of the method
 * name to be given and supports lining up multiple properties ending with the sorting direction. So the following
 * method ends are valid: {@code LastnameUsernameDesc}, {@code LastnameAscUsernameDesc}.
 *
 * @author Oliver Gierke
 */
public class OrderBySource {

    private static final String BLOCK_SPLIT = "(?<=Asc|Desc)(?=\\p{Lu})";
    private static final Pattern DIRECTION_SPLIT = Pattern.compile("(.+?)(Asc|Desc)?$");
    private static final String INVALID_ORDER_SYNTAX = "Invalid order syntax for part %s!";
    private static final Set<String> DIRECTION_KEYWORDS = ContainerUtil.set("Asc", "Desc");

    private final List<Sort.Order> orders;
    private final String mySource;

    /**
     * Creates a new {@link OrderBySource} for the given clause, checking the property referenced exists on the given
     * type.
     *
     * @param clause      must not be {@literal null}.
     * @param domainClass can be {@literal null}.
     */
    public OrderBySource(@NotNull String clause, @NotNull PsiClass domainClass) {
        mySource = clause;

        this.orders = new ArrayList<>();

        if (StringUtil.isNotEmpty(clause)) {
            for (String part : clause.split(BLOCK_SPLIT)) {
                Matcher matcher = DIRECTION_SPLIT.matcher(part);
                if (matcher.find()) {
                    String propertyString = matcher.group(1);
                    String directionString = matcher.group(2);

                    if (DIRECTION_KEYWORDS.contains(propertyString) && directionString == null) {
                        // No property, but only a direction keyword
                        // spring data: throw new IllegalArgumentException(String.format(INVALID_ORDER_SYNTAX, part));
                        Sort.Direction direction = StringUtil.isNotEmpty(propertyString) ? Sort.Direction.fromString(propertyString) : null;
                        this.orders.add(createOrder("", direction, domainClass, part));
                    } else {
                        Sort.Direction direction = StringUtil.isNotEmpty(directionString) ? Sort.Direction.fromString(directionString) : null;
                        this.orders.add(createOrder(propertyString, direction, domainClass, part));
                    }
                }
            }
        }
    }

    /**
     * Creates an {@link Sort.Order} instance from the given property source, direction and domain class. If the domain class
     * is given, we will use it for nested property traversal checks.
     *
     * @param propertySource
     * @param direction
     * @param domainClass    can be {@literal null}.
     * @param sortExpression
     * @return
     * @see PropertyPath#from(String, PsiType)
     */
    private static Sort.Order createOrder(String propertySource,
                                          Sort.Direction direction,
                                          @NotNull PsiClass domainClass,
                                          String sortExpression) {
        PropertyPath propertyPath = PropertyPath.from(propertySource, JavaPsiFacade
                .getElementFactory(domainClass.getProject()).createType(domainClass, PsiSubstitutor.EMPTY));
        return new Sort.Order(direction, propertyPath, propertySource, sortExpression);
    }

    /**
     * Returns the clause as {@link Sort}.
     *
     * @return the {@link Sort} or null if no orders found.
     */
    public Sort toSort() {
        return this.orders.isEmpty() ? null : new Sort(this.orders);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ORDER_BY ('" + mySource + "')";
    }

    public List<Sort.Order> getOrders() {
        return orders;
    }

    public String getSource() {
        return mySource;
    }
}

