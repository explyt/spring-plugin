package com.app;

import org.springframework.stereotype.Component;

@Component
public class User {
    private final Foo foo;
    private final Bar bar;
    private final Baz baz;

    public User(Foo foo, Bar bar, Baz baz) {
        this.foo = foo;
        this.bar = bar;
        this.baz = baz;
    }
}
