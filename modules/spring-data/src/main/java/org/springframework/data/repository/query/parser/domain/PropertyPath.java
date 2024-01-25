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
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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

    /**
     * Creates a leaf {@link PropertyPath} (no nested ones with the given name and owning type.
     *
     * @param name       must not be {@literal null} or empty.
     * @param owningType must not be {@literal null}.
     * @param base       the {@link PropertyPath} previously found.
     */
    PropertyPath(@NotNull String name, @Nullable PsiType owningType, @NotNull List<PropertyPath> base) {
        String propertyName = name.matches(ALL_UPPERCASE) ? name : StringUtil.decapitalize(name);

        this.owningType = owningType;
        this.isCollection = false; // todo: !!! propertyType.isCollectionLike();
        this.type = getActualType(owningType, propertyName); // see TypeInformation<?> getActualType();
        this.name = propertyName;
    }

    @Nullable
    private static PsiType getActualType(@Nullable PsiType psiType, String propertyName) {
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

    @Nullable
    public static PsiType getActualType(String propertyName, PsiClass psiClass) {
        PsiField field = PropertyUtilBase.findPropertyField(psiClass, propertyName, false);
        if (field != null) return field.getType();
        PsiMethod getter = PropertyUtilBase.findPropertyGetter(psiClass, propertyName, false, true);
        return getter != null ? PropertyUtilBase.getPropertyType(getter) : null;
    }

    /**
     * Returns the owning type of the {@link PropertyPath}.
     *
     * @return the owningType will never be {@literal null}.
     */
    @Nullable
    public PsiType getOwningType() {
        return owningType;
    }

    /**
     * Returns the name of the {@link PropertyPath}.
     *
     * @return the name will never be {@literal null}.
     */
    public String getSegment() {
        return name;
    }

    /**
     * Returns the leaf property of the {@link PropertyPath}.
     *
     * @return will never be {@literal null}.
     */
    public PropertyPath getLeafProperty() {

        PropertyPath result = this;

        while (result.hasNext()) {
            result = result.next();
        }

        return result;
    }

    /**
     * Returns the type of the property will return the plain resolved type for simple properties, the component type for
     * any {@link Iterable} or the value type of a {@link Map} if the property is one.
     *
     * @return
     */
    @Nullable
    public PsiType getType() {
        return this.type;
    }

    /**
     * Returns the next nested {@link PropertyPath}.
     *
     * @return the next nested {@link PropertyPath} or {@literal null} if no nested {@link PropertyPath} available.
     * @see #hasNext()
     */
    public PropertyPath next() {
        return next;
    }

    /**
     * Returns whether there is a nested {@link PropertyPath}. If this returns {@literal true} you can expect
     * {@link #next()} to return a non- {@literal null} value.
     *
     * @return
     */
    public boolean hasNext() {
        return next != null;
    }

    /**
     * Returns the {@link PropertyPath} in dot notation.
     *
     * @return
     */
    public String toDotPath() {
        if (hasNext()) {
            return getSegment() + "." + next().toDotPath();
        }
        return getSegment();
    }

    /**
     * Returns whether the {@link PropertyPath} is actually a collection.
     *
     * @return
     */
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

    /*
     * (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
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

    /**
     * Extracts the {@link PropertyPath} chain from the given source {@link String} and {@link PsiType}.
     *
     * @param source must not be {@literal null}.
     * @param type
     * @return
     */
    public static PropertyPath from(@NotNull String source, @Nullable PsiType type) {

        //Assert.hasText(source, "Source must not be null or empty!");
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

    /**
     * Creates a new {@link PropertyPath} as subordinary of the given {@link PropertyPath}.
     *
     * @param source
     * @param base
     * @return
     */
    private static PropertyPath create(String source, Stack<PropertyPath> base) {

        PropertyPath previous = base.peek();

        PropertyPath propertyPath = create(source, previous.type, base);
        previous.next = propertyPath;
        return propertyPath;
    }

    /**
     * Factory method to create a new {@link PropertyPath} for the given {@link String} and owning type. It will inspect
     * the given source for camel-case parts and traverse the {@link String} along its parts starting with the entire one
     * and chewing off parts from the right side then. Whenever a valid property for the given class is found, the tail
     * will be traversed for subordinary properties of the just found one and so on.
     *
     * @param source
     * @param type
     * @return
     */
    private static PropertyPath create(String source, PsiType type, List<PropertyPath> base) {
        return create(source, type, "", base);
    }

    /**
     * Tries to look up a chain of {@link PropertyPath}s by trying the givne source first. If that fails it will split the
     * source apart at camel case borders (starting from the right side) and try to look up a {@link PropertyPath} from
     * the calculated head and recombined new tail and additional tail.
     *
     * @param source
     * @param type
     * @param addTail
     * @return
     */
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

    @Nullable
    private static PropertyPath getImplicitExpressionPropertyPath(String source, PsiType type, String addTail, List<PropertyPath> base) {
        // findByAddressZipCode (property expression: address.zipCode).
        // can be expressed explicitly: findByAddress_ZipCode (see: PropertyPath.from())
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

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("%s.%s", owningType.getCanonicalText(), toDotPath());
    }
}