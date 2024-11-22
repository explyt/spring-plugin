/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd
 * and are protected by copyright and other intellectual property laws.
 *
 * Original work: org.springframework.data.repository.query.parser.OrderBySource
 * Available during publication at https://github.com/spring-projects/spring-data-commons/blob/3.0.x/src/main/java/org/springframework/data/repository/query/parser/OrderBySource.java
 * Licensed under the Apache License, Version 2.0:
 *     Copyright © 2013-2024 Oliver Gierke, Mark Paluch,
 *     Christoph Strobl, Mariusz Mączkowski, and other contributors.
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
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiSubstitutor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyPath;

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
    public static final Set<String> DIRECTION_KEYWORDS = Set.of("Asc", "Desc");

    private final List<Sort.Order> orders;
    private final String mySource;

    public OrderBySource(String clause, PsiClass domainClass, int offset) {
        mySource = clause;

        this.orders = new ArrayList<>();

        if (StringUtil.isNotEmpty(clause)) {
            int currentOffset = 0;
            for (String part : clause.split(BLOCK_SPLIT)) {
                Matcher matcher = DIRECTION_SPLIT.matcher(part);
                if (matcher.find()) {
                    currentOffset = StringUtil.indexOf(clause, part, currentOffset);
                    String propertyString = matcher.group(1);
                    String directionString = matcher.group(2);

                    if (DIRECTION_KEYWORDS.contains(propertyString) && directionString == null) {
                        Sort.Direction direction = StringUtil.isNotEmpty(propertyString) ? Sort.Direction.fromString(propertyString) : null;
                        this.orders.add(createOrder("", direction, domainClass, part, offset + currentOffset));
                    } else {
                        Sort.Direction direction = StringUtil.isNotEmpty(directionString) ? Sort.Direction.fromString(directionString) : null;
                        this.orders.add(createOrder(propertyString, direction, domainClass, part, offset + currentOffset));
                    }
                }
                currentOffset += Math.max(1, part.length());
            }
        }
    }

    private static Sort.Order createOrder(
            String propertySource, Sort.Direction direction, PsiClass domainClass, String sortExpression, int offset
    ) {
        PropertyPath propertyPath = PropertyPath.from(propertySource, JavaPsiFacade
                .getElementFactory(domainClass.getProject()).createType(domainClass, PsiSubstitutor.EMPTY));
        return new Sort.Order(direction, propertyPath, propertySource, sortExpression, offset);
    }

    public Sort toSort() {
        return this.orders.isEmpty() ? null : new Sort(this.orders);
    }

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

