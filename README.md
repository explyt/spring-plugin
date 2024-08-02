[Spring Explyt plugin](https://explyt.com/plugin/) (former Esprito Spring Tools) accelerates development of your Spring and Spring Boot applications by minimizing routine tasks, supporting Spring Boot profiles, configurations and properties, highlighting Spring Core Beans dependencies and usages. Moreover, it validates that Spring Beans injections are used correctly.

Spring Explyt plugin provides solid support for Spring Data, Spring Web, Spring Reactive Web, Spring AOP (in progress) including AspectJ, Spring Security, Spring Integration (partly).


The plugin creates a developer-friendly environment for programmers working with IntelliJ IDEA Community Edition, Android Studio, Aqua, and helps you better understand the context of your Spring applications.

It highlights the mistakes, misbehavior, and possible problems and typos in your code using more than 50 inspections.

Spring Explyt plugin allows you to create Spring projects inside IDE using [Spring Initializr](https://start.spring.io/).

It supports Java and other JVM languages (Kotlin, Scala, etc), enables syntax support  for JPQL, OpenAPI (swagger).

To use Spring Explyt plugin in IntelliJ IDEA Ultimate please disable Spring plugin (conflict functionality). The plugin supports Spring 6 and higher.

This plugin is available for free for non-commercial use. 

This plugin is available free of charge.

* [NON-COMMERCIAL LICENSE](https://github.com/explyt/spring-plugin/blob/main/LICENSE.md)
* [COMMERCIAL LICENSE](https://github.com/explyt/spring-plugin/blob/main/COMMERCE_LICENSE.md) - In 2024, it is free, but you will need to obtain a license key from our support team. Please contact us at [sales@explyt.com](mailto:sales@explyt.com).

Please feel free to report issues in [GitHub repository](https://github.com/explyt/spring-plugin/issues).


## Key features

To use Spring Explyt plugin in IntelliJ IDEA Ultimate please disable Spring plugin (conflict functionality). The plugin supports Spring 6 and higher.

### Spring Initializr
* Integrate with https://start.spring.io/ initializr
* create Spring project inside IDE

### Spring Core

* Spring Beans
  - @Bean and meta-annotations 
  - @Component and meta-annotations 
  - @Qualifier and meta-annotations 
    * Support for @Primary, @Order  
  - @DependsOn and meta-annotations
  - @Scope - standard + detect custom scopes
  - @ComponentScan and @Import  and meta-annotations
  - @Value and meta-annotations
  - @Lookup and meta-annotations
  - JSR-330: javax.inject, and jakarta.inject annotations
  - context.getBean bean detection
  - Line Marker, show and navigate to usages, show and navigate to declarations
  - Navigate from/to BeanFactories
  - Find bean usages
  - @AliasFor annotations
  - Inspections
  - Statically injected beans
  - @Environment and local env variables are supported
* Support Lombok library for java spring projects  
* Spring Profiles
  - @Profile and meta-annotations
  - Supported through Run Configurations - you have to choose Run Configuration with profiles to use the Profile support
  - spring.profiles.active - from spring boot configuration.
  - Used or unused profile marked beans
  - Inspections
* Spring Events
  - @EventListener and meta-annotations
  - Listener, Publisher and show and navigate to usages in code are marked
  - Inspections
* Scheduling
  - @Scheduled and meta-annotations
  - Cron templates and examples
  - Inspections
* Async
  - @Async and meta-annotations
* Resource
  - @ImportResource and meta-annotations
  - Support resource inspections and autocompletion for ResourceLoader and ResourceUtils
  - Autocompletion
    * Classpath
    * File
    * Url
  - Inspections
* Cache
  - Support @Cache* and other meta-annotations
  - Inspections

### Spring Boot

* Auto-detect Run configurations with @SpringBootApplication
  - From Code
  - From Gradle
  - From Maven
* Configuration Properties
  - @ConfigurationProperties and meta-annotations
    * @NestedConfigurationProperty
    * @DeprecatedConfigurationProperty
  - @PropertySource and meta-annotations
    * Inspections
    * Autocompletion
  - @DynamicPropertySource and meta-annotations
  - @Value property templates
  - application.yaml / application.properties
    * Profile-specific: application-profile.yaml, bootstrap.yaml and properties equivalent
    * Support Relaxed Binding property values
    * Autocompletion
      - Properties
      - Templates
      - Packages
      - Classpath
      - Property values by Value Providers and Hints from additional-spring-configuration-metadata.json
      - Property values by @ConfigurationProperties class
    * highlighting
      - usages
      - non-used properties
      - deprecations
      - errors
    * documentation
      - escription
      - efault values
      - eprecations
      - ConfigurationProperties comments
      - dditional-spring-configuration-metadata.json descriptions
    * Inspections
      - Type Validation
      - Value Hints
      - Value Providers
    * Navigate to usages
    * Navigate to additional-spring-configuration-metadata.json
    * Intention - create additional-spring-configuration-metadata.json hint
  - Metadata.json
    * spring-configuration-metadata.json
    * additional-spring-configuration-metadata.json
    * Parse and Highlight json
    * Navigate to key usages
    * Inspections
* Auto-Configurations and Spring-Boot-Starters
  - @ConditionalOn:
    * @ConditionalOnClass
    * @ConditionalOnBean
    * @ConditionalOnProperty
    * @ConditionalOnMissingBean
    * @DependsOn
  - spring.factories
    * Setup EnableAutoConfiguration
    * File highlight and completion
    * Inspection migrate to AutoConfiguration.imports
  - org.springframework.boot.autoconfigure.AutoConfiguration.imports
    * File highlight and completion
    * Inspections
* Spring Boot Tests
  - @SpringBootTest and meta-annotations
  - @TestPropertySource and meta-annotations

### Spring Web
* Spring Web RequestMapping Controllers
  - Navigation to controllers:
    * From MockMvc
    * From WebTestClient
    * From tests
    * From OpenApi files
  - Show usages
  - Url Link detections
  - Inspections
  - @InitBinder
  - Validate Controller’s methods and parameters
  - WebController
  - Reactive Controllers
    * Support Mono/Flux
    * Support Kotlin suspend methods (coroutines)
* OpenApi
  - Detect openapi file
  - highlight and navigation
  - Autocompletion
* MockMvc
  - Inspections

### Spring Data
* JPQL - language support
  - injection to @Query
  - Autocompletion
  - Inspections
* Spring Data Repository support:
  - JpaRepository
  - CrudRepository
  - RepositoryRestResource
  - Autocomplete (generate) Spring data Repository interface methods

### Spring Security
* Detect UserDetailsService
  - And for test: WithUserDetails

### Spring Integration
* nullChannel
* errorChannel
* integrationFlowContext

### Spring AOP
* Basic support Spring AOP
  - Inspections
* Basic support AspectJ
  - All aspectJ annotations
