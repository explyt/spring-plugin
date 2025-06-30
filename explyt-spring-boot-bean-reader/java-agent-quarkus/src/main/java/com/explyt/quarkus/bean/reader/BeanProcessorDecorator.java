/*
 * Copyright © 2025 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.quarkus.bean.reader;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanRegistrar;
import org.jboss.jandex.AnnotationTarget;
import tech.ytsaurus.spyt.patch.annotations.AddMethod;
import tech.ytsaurus.spyt.patch.annotations.Decorate;
import tech.ytsaurus.spyt.patch.annotations.DecoratedMethod;
import tech.ytsaurus.spyt.patch.annotations.OriginClass;

import java.util.List;

import static com.explyt.quarkus.bean.reader.Constants.*;


@Decorate
@OriginClass("io.quarkus.arc.processor.BeanProcessor")
public class BeanProcessorDecorator {

    @DecoratedMethod
    public BeanRegistrar.RegistrationContext registerBeans() {
        BeanRegistrar.RegistrationContext context = __registerBeans();
        System.out.println(EXPLYT_BEAN_INFO_START);
        printBeans(context);
        System.out.println(EXPLYT_BEAN_INFO_END);
        throw new RuntimeException(SPRING_EXPLYT_ERROR_MESSAGE);
    }

    private BeanRegistrar.RegistrationContext __registerBeans() {
        return null;
    }

    @AddMethod
    private void printBeans(BeanRegistrar.RegistrationContext context) {
        if (context == null) return;

        List<BeanInfo> beans = context.beans().collect();
        for (BeanInfo bean : beans) {
            AnnotationTarget annotationTarget = bean.getTarget().orElse(null);
            if (annotationTarget == null) continue;
            String[] split = annotationTarget.toString().split(" ");
            String methodName = split.length > 1 ? split[1] : "";
            String methodType = methodName.isEmpty() ? "" : split[0];
            System.out.println(
                    EXPLYT_BEAN_INFO +
                            "{\"className\": \"" + split[0] + "\"," +
                            "\"beanName\": \"" + bean.getName() + "\"," +
                            "\"methodName\": " + methodName + "," +
                            "\"methodType\": " + methodType + "," +
                            "\"scope\": \"" + bean.getScope().getDotName() + "\"," +
                            "\"primary\": " + bean.getPriority() + "}"
            );
        }
        System.out.println(beans.size());
    }
}
