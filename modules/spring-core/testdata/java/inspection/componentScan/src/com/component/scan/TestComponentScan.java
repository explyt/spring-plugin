package com.component.scan;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(scanBasePackages = "a.1.3")
@SpringBootApplication(scanBasePackages = "com.1.3")
@SpringBootApplication(scanBasePackages = "com.1.4")
@SpringBootApplication(scanBasePackages = "com.1.3.")
@SpringBootApplication(scanBasePackages = "com.1.3.3")
@SpringBootApplication(scanBasePackages = "com.?.3")
@SpringBootApplication(scanBasePackages = "com.?.?")
@SpringBootApplication(scanBasePackages = "*.?.?")
@SpringBootApplication(scanBasePackages = "*.1.3")
@SpringBootApplication(scanBasePackages = "*.1.3.")
@SpringBootApplication(scanBasePackages = "com.2.*")
@SpringBootApplication(scanBasePackages = "com.2.??")
@SpringBootApplication(scanBasePackages = "com.2.a")
@SpringBootApplication(scanBasePackages = "com.?.4")
@SpringBootApplication("com.1.3")
@SpringBootApplication(scanBasePackages = "com.?7.?")
@SpringBootApplication({"com.2.4", "com.?.?", "com.2.6"})
@SpringBootApplication(scanBasePackages = {"com.2.4", "com.?.?", "com.2.6"})
@ComponentScan(basePackages = "a.1.3")
@ComponentScan(basePackages = "com.1.3")
@ComponentScan(basePackages = "com.1.4")
@ComponentScan(basePackages = "com.1.3.")
@ComponentScan(basePackages = "com.1.3.3")
@ComponentScan(basePackages = "com.?.3")
@ComponentScan(basePackages = "com.?.?")
@ComponentScan(basePackages = "*.?.?")
@ComponentScan(basePackages = "*.1.3")
@ComponentScan(basePackages = "*.1.3.")
@ComponentScan(basePackages = "com.2.*")
@ComponentScan(basePackages = "com.2.??")
@ComponentScan(basePackages = "com.2.a")
@ComponentScan(basePackages = "com.?.4")
@ComponentScan("com.1.3")
@ComponentScan(basePackages = "com.?7.?")
@ComponentScan({"com.2.4", "com.?.?", "com.2.6"})
@ComponentScan(basePackages = {"com.2.4", "com.?.?", "com.2.6"})
@ComponentScan("**.3")
@SpringBootApplication("**.4")
public class TestComponentScan {
}

class ConstPackageComponentScan {
    static final String BASE_PACKAGE = "com.component.scan";
    static final String INVALID_BASE_PACKAGE = "com.component.invalid";

    @ComponentScan(BASE_PACKAGE)
    static class ValidScalar {
    }

    @ComponentScan(basePackages = {BASE_PACKAGE})
    @org.springframework.boot.context.properties.ConfigurationPropertiesScan(basePackages = {BASE_PACKAGE})
    @SpringBootApplication(scanBasePackages = {BASE_PACKAGE})
    static class ValidArray {
    }

    @ComponentScan(INVALID_BASE_PACKAGE)
    static class InvalidScalar {
    }

    @ComponentScan(basePackages = {INVALID_BASE_PACKAGE})
    @org.springframework.boot.context.properties.ConfigurationPropertiesScan(basePackages = {INVALID_BASE_PACKAGE})
    @SpringBootApplication(scanBasePackages = {INVALID_BASE_PACKAGE})
    static class InvalidArray {
    }
}

@ComponentScan(ScanConstants.BASE_PACKAGE)
@ComponentScan(basePackages = {ScanConstants.INVALID_BASE_PACKAGE})
class CrossFileConstPackageComponentScan {
}

class NonConstPackageComponentScan {
    static String nonConstPackage() {
        return "com.component.invalid";
    }

    // Not a compile-time constant: the inspection must stay silent instead of parsing the source text.
    @ComponentScan(nonConstPackage())
    static class NonConstArgument {
    }
}
