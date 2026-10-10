/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.java

import com.explyt.spring.core.service.conditional.ConditionFixtures
import com.intellij.testFramework.fixtures.CodeInsightTestFixture

class JavaConditionFixtures(private val fixture: CodeInsightTestFixture) : ConditionFixtures {

    override fun addApplication() = addJava(
        "Application",
        """
        package com.app;

        import org.springframework.boot.autoconfigure.SpringBootApplication;

        @SpringBootApplication
        public class Application {}
        """
    )

    override fun addPlainComponent() = addJava(
        "PlainService",
        """
        package com.app;

        import org.springframework.stereotype.Component;

        @Component
        public class PlainService {}
        """
    )

    override fun addPropertyGatedConfiguration(className: String) = addJava(
        className,
        """
        package com.app;

        import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
        import org.springframework.context.annotation.Configuration;

        @Configuration
        @ConditionalOnProperty(name = "feature.sync", havingValue = "true")
        public class $className {}
        """
    )

    override fun addCustomConditionConfiguration() {
        addJava(
            "MyCondition",
            """
            package com.app;

            import org.springframework.context.annotation.Condition;
            import org.springframework.context.annotation.ConditionContext;
            import org.springframework.core.type.AnnotatedTypeMetadata;

            public class MyCondition implements Condition {
                @Override
                public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
                    return false;
                }
            }
            """
        )
        addJava(
            "CustomConditionConfig",
            """
            package com.app;

            import org.springframework.context.annotation.Conditional;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @Conditional(MyCondition.class)
            public class CustomConditionConfig {}
            """
        )
    }

    override fun addUnknownProfileConfiguration() = addJava(
        "UnknownProfileConfig",
        """
        package com.app;

        import org.springframework.context.annotation.Configuration;
        import org.springframework.context.annotation.Profile;

        @Configuration
        @Profile(UNKNOWN_PROFILE)
        public class UnknownProfileConfig {}
        """
    )

    override fun addMissingBeanConfiguration() {
        addJava(
            "Missing",
            """
            package com.app;

            public class Missing {}
            """
        )
        addJava(
            "MissingBeanConfig",
            """
            package com.app;

            import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @ConditionalOnBean(Missing.class)
            public class MissingBeanConfig {}
            """
        )
    }

    override fun addConditionalOuterConfiguration() {
        addJava(
            "OuterService",
            """
            package com.app;

            public class OuterService {}
            """
        )
        addJava(
            "OuterConfig",
            """
            package com.app;

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @ConditionalOnProperty(name = "outer.enabled", havingValue = "true")
            public class OuterConfig {
                @Bean
                public OuterService outerService() {
                    return new OuterService();
                }
            }
            """
        )
    }

    override fun addMixedConditionsConfiguration() {
        addJava(
            "MixedCondition",
            """
            package com.app;

            import org.springframework.context.annotation.Condition;
            import org.springframework.context.annotation.ConditionContext;
            import org.springframework.core.type.AnnotatedTypeMetadata;

            public class MixedCondition implements Condition {
                @Override
                public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
                    return true;
                }
            }
            """
        )
        addJava(
            "MixedConditionsConfig",
            """
            package com.app;

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Conditional;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @Conditional(MixedCondition.class)
            @ConditionalOnProperty(name = "mixed.enabled", havingValue = "true")
            public class MixedConditionsConfig {}
            """
        )
    }

    override fun addSyncAdminController() = addJava(
        "SyncAdminController",
        """
        package com.app;

        import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
        import org.springframework.stereotype.Controller;

        @Controller
        @ConditionalOnProperty(prefix = "admin.sync", name = "enabled")
        public class SyncAdminController {}
        """
    )

    private fun addJava(className: String, source: String) {
        fixture.addFileToProject("com/app/$className.java", source.trimIndent())
    }
}
