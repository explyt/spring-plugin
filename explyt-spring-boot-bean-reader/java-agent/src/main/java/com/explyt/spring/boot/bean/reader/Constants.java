/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.boot.bean.reader;

public interface Constants {
    String EXPLYT_BEAN_INFO_START = "ExplytBeanInfoStart";
    String EXPLYT_BEAN_INFO_END = "ExplytBeanInfoEnd";
    String EXPLYT_BEAN_INFO = "ExplytBeanInfo:";
    String EXPLYT_AOP_INFO = "ExplytBeanAopInfo:";
    String SPRING_EXPLYT_ERROR_MESSAGE = "I am Explyt Spring";
    String SKIP_INIT_PARAM = "explyt.spring.skip.init"; //skip init. only for get BeanDefinitions
}
