/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd
 * and are protected by copyright and other intellectual property laws.
 *
 * Original work: org.springframework.data.mapping.PropertyPath
 * Available during publication at https://github.com/spring-projects/spring-data-commons/blob/3.0.x/src/main/java/org/springframework/data/mapping/PropertyPath.java
 * Licensed under the Apache License, Version 2.0:
 *     Copyright © 2011-2024 Oliver Gierke, Christoph Strobl,
 *     Mark Paluch, Mariusz Mączkowski, Johannes Englmeier, and other contributors.
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
package org.springframework.data.mapping;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.containers.Stack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstraction of a {@link PropertyPath} of a domain class.
 *
 * @author Oliver Gierke
 */
public class PropertyPath implements Iterable<PropertyPath> {

    private static final String DELIMITERS = "_\\.";
    private static final String ALL_UPPERCASE = "[A-Z0-9._$]+";
    private static final Pattern SPLITTER = Pattern.compile("(?:[%s]?([%s]*?[^%s]+))".replaceAll("%s", DELIMITERS));

    private final PsiType owningType;
    private final String name;
    private final PsiType type;
    private final boolean isCollection;

    private PropertyPath next;

    PropertyPath(String name, PsiType owningType, List<PropertyPath> base) {
        String propertyName = name.matches(ALL_UPPERCASE) ? name : StringUtil.decapitalize(name);
        this.owningType = owningType;
        this.isCollection = false;

        this.type = getActualType(owningType, propertyName);
        this.name = propertyName;
    }

    private static PsiType getActualType(PsiType psiType, String propertyName) {
        if (psiType instanceof PsiClassType) {
            PsiClass psiClass = ((PsiClassType) psiType).resolve();
            if (psiClass == null) return null;

            if (InheritanceUtil.isInheritor(psiClass, CommonClassNames.JAVA_UTIL_COLLECTION)) {
                final PsiType genericType = PsiUtil.substituteTypeParameter(psiType, CommonClassNames.JAVA_UTIL_COLLECTION, 0, true);
                if (genericType != null) {
                    return getActualType(genericType, propertyName);
                }
            }
            return getActualType(propertyName, psiClass);
        }
        return null;
    }

    public static PsiType getActualType(String propertyName, PsiClass psiClass) {
        PsiField field = PropertyUtilBase.findPropertyField(psiClass, propertyName, false);
        if (field != null) return field.getType();

        PsiMethod getter = PropertyUtilBase.findPropertyGetter(psiClass, propertyName, false, true);
        return getter != null ? PropertyUtilBase.getPropertyType(getter) : null;
    }

    public String getSegment() {
        return name;
    }

    public PsiType getType() {
        return this.type;
    }

    public PropertyPath next() {
        return next;
    }

    public boolean hasNext() {
        return next != null;
    }

    public String toDotPath() {
        if (hasNext()) {
            return getSegment() + "." + next().toDotPath();
        }
        return getSegment();
    }

    public boolean isCollection() {
        return isCollection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropertyPath path = (PropertyPath) o;

        if (isCollection != path.isCollection) return false;
        return Objects.equals(owningType, path.owningType) &&
                Objects.equals(name, path.name) &&
                Objects.equals(type, path.type) &&
                Objects.equals(next, path.next);
    }

    @Override
    public int hashCode() {
        int result = owningType != null ? owningType.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (isCollection ? 1 : 0);
        result = 31 * result + (next != null ? next.hashCode() : 0);
        return result;
    }

    @Override
    public Iterator<PropertyPath> iterator() {
        return new Iterator<>() {

            private PropertyPath current = PropertyPath.this;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public PropertyPath next() {
                PropertyPath result = current;
                this.current = current.next();
                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static PropertyPath from(String source, PsiType type) {
        if (StringUtil.isEmptyOrSpaces(source)) return create("", type, new Stack<>());

        List<String> iteratorSource = new ArrayList<>();
        Matcher matcher = SPLITTER.matcher("_" + source);

        while (matcher.find()) {
            iteratorSource.add(matcher.group(1));
        }

        Iterator<String> parts = iteratorSource.iterator();

        PropertyPath result = null;
        Stack<PropertyPath> current = new Stack<>();

        while (parts.hasNext()) {
            if (result == null) {
                result = create(parts.next(), type, current);
                current.push(result);
            } else {
                current.push(create(parts.next(), current));
            }
        }

        return result != null ? result : create("", type, new Stack<>());
    }

    private static PropertyPath create(String source, Stack<PropertyPath> base) {

        PropertyPath previous = base.peek();

        PropertyPath propertyPath = create(source, previous.type, base);
        previous.next = propertyPath;
        return propertyPath;
    }

    private static PropertyPath create(String source, PsiType type, List<PropertyPath> base) {
        return create(source, type, "", base);
    }

    private static PropertyPath create(String source, PsiType type, String addTail, List<PropertyPath> base) {
        PropertyPath current = new PropertyPath(source, type, base);
        if (current.getType() == null) {
            PropertyPath path = getImplicitExpressionPropertyPath(source, type, addTail, base);
            if (path != null) return path;
        }

        if (!base.isEmpty()) {
            base.get(base.size() - 1).next = current;
        }
        List<PropertyPath> newBase = new ArrayList<>(base);
        newBase.add(current);
        if (StringUtil.isNotEmpty(addTail)) {
            current.next = create(addTail, current.type, newBase);
        }
        return current;
    }

    private static PropertyPath getImplicitExpressionPropertyPath(String source, PsiType type, String addTail, List<PropertyPath> base) {
        Pattern pattern = Pattern.compile("\\p{Lu}+\\p{Ll}*$");
        Matcher matcher = pattern.matcher(source);

        if (matcher.find() && matcher.start() != 0) {
            int position = matcher.start();
            String head = source.substring(0, position);
            String tail = source.substring(position);

            PropertyPath path = create(head, type, tail + addTail, base);
            if (path.getType() != null) return path;
        }
        return null;
    }


    @Override
    public String toString() {
        return owningType.getCanonicalText() + "." + toDotPath();
    }
}