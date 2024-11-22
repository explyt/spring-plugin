package com;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

interface I {}

@Component
class A implements I {}

@Service
class B implements I {}

@Configuration
public class DependsOnConfiguration {
    @DependsOn("a")
    @Bean I bDependentOnA() { return new B(); }

    @DependsOn("wrongBeanNameAtMethod") // Ultimate handles it wrong. There is no navigation, nor error bean highlight
    @Bean I bDependentOnWrong() { return new B(); }
}

//region Valid
@Component
@DependsOn("a")
class DependentOnABean {}

@Component
@DependsOn({"a", "b", E.CONST_TO_VALID_BEAN})
class DependentOnMultipleBeans {}
//endregion

//region Invalid
@Component
@DependsOn("wrongBeanNameAtClass")
class DependentOnABeanErr {}

@Component
@DependsOn({"wrongBeanNameListedAtClass", "a", E.CONST_TO_NON_EXISTING_BEAN})
class DependentOnMultipleBeansErr {}
//endregion

@Component
class E implements I {
    public static final java.lang.String CONST_TO_VALID_BEAN = "b";
    public static final java.lang.String CONST_TO_NON_EXISTING_BEAN = "nope";
}

