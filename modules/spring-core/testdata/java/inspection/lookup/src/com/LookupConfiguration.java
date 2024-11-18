package com;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class LookupConfiguration {
    @Lookup
    LookBeanA someName() {
        return null;
    }

    @Lookup("lookBeanA")
    LookBeanA lookBeenA() {
        return null;
    }

    @Lookup("lookBeanA")
        // reports "Expected bean of 'LookBeanB' type but found 'LookBeanA'"
    LookBeanB lookBeanB() {
        return null;
    }

    @Lookup("unknown")
        // reports "Cannot resolve bean 'unknown'"
    LookBeanB unknown() {
        return null;
    }
}

@Component
class LookBeanA {
}

@Component
class LookBeanB {
}
