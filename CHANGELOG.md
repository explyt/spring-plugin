<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Explyt Spring Changelog

## [Unreleased]

### Spring Web
- fix: List the functional routes of a project that declares them with `router { }` or with the servlet stack (WebMvc.fn): only the coroutine `coRouter { }` of WebFlux was recognised, so a reactive router written without coroutines contributed nothing, and a Spring MVC project — which carries no Reactor, and whose routes are built on `org.springframework.web.servlet.function` — showed no functional route at all, neither in the Explyt Endpoints tool window nor in endpoint search. Servlet routes are now listed under Spring MVC rather than WebFlux, and a Java builder route whose path constant holds several values contributes one endpoint per path instead of only the first (#415)
- fix: List a functional route declared as `HEAD`, `OPTIONS` or the generic `method(HttpMethod.GET, handler)` form: only five of the seven verbs the router DSL declares were recognised, so those routes were missing from the Explyt Endpoints tool window and had no endpoint gutter, and the generic form — which carries its verb in the argument rather than in the called name — was skipped entirely. The tool window's HttpType filter now also offers every verb it can display: `HEAD`, `OPTIONS`, `TRACE` and `CONNECT` were rendered with their own icons yet could never be selected (#416)
- fix: List a WebFlux functional route under the path it actually serves when the route URI is not a string literal spelled out in the call: a path held in a constant, or a list of constants declared once and registered with `PROXIED_PATHS.forEach { GET(it, handler::handle) }`, resolved to nothing, so the Explyt Endpoints tool window showed a bare `GET`/`POST` row with an empty path and ten routes collapsed into two. A route whose path still cannot be resolved no longer reaches the endpoint model at all — it used to be normalised to `/` and answer lookups for the application root in the gutter, in endpoint usage search and in the MCP endpoint tools (#414)
- fix: List an Actuator endpoint once, with a server-relative path, instead of once per module that sets `management.server.port` and with the port pasted into the path: a shared endpoint appeared twice in the Explyt Endpoints tool window — the second row reading `http://localhost:0/...` when the port came from a test configuration binding an ephemeral port — and the host made the path unmatchable, so no URL string resolved to an Actuator endpoint whenever a management port was set

### Spring AI
- fix: Start an `explyt_trace_spring_call_chain` trace from any line of the method, instead of only from a line of its body: the declaration line is the coordinate every other Explyt tool hands out — `explyt_find_spring_endpoint` reports a handler at its signature — and passing it through answered `no method found`, so the two tools disagreed about the same Spring method and the caller had to guess the convention. Only the line's first offset was examined, and that offset is the indentation whitespace, which is a child of the method inside a body but a sibling of it on the declaration line. A line that genuinely belongs to no method is still refused, now naming the nearest method declarations in that file instead of failing bare (#435)
- fix: Return every endpoint `explyt_get_spring_endpoint_contract` counted, instead of dropping the ones whose handler is not an annotated method: a functional route (`coRouter`/`router`/`RouterFunctions.route()`), an OpenAPI declaration and an operation-less Actuator endpoint were counted into `totalCount` and then silently removed, so a route the endpoint model resolves answered `{"totalCount": 1, "truncated": false, "endpoints": []}` — which reads as "no such route" — and a mixed match returned only the unrelated longer paths, naming a controller that does not serve the URL. Such an endpoint now comes back with `contractStatus: PARTIAL`, the bean factory that registers it, the handler function as `serviceCall`, the path variables its URL template declares, and a `contractUnavailableReason` naming what has to be read at the source. A Java builder route no longer yields a contract fabricated from the registration signature, which reported the injected handler bean as a request parameter and `RouterFunction<ServerResponse>` as the response type (#425)
- fix: List the endpoint matching the queried pattern exactly before one that merely contains it in `explyt_find_spring_endpoint` and `explyt_get_spring_endpoint_contract`: matching is forgiving by design, and ordering purely by path specificity — which breaks ties by descending length — put a longer unrelated path first, where the caller reads its answer (#425)
- fix: Make endpoint contracts identify the first application service call instead of a preceding framework or Kotlin stdlib call, preserve Kotlin nullability inside generic response fields, and report multipart MVC parts with their wire names and requiredness (#331)

### Spring Core
- fix: Keep a Native Context Mode sync that has already read the application's beans instead of discarding it with `IndexNotReadyException`: the sync builds the project while it resolves, and the build output triggers a file-system refresh that starts indexing in every open project, so the resolver's own class lookups died on the first index query — after the application had started, reported its context and exited. Waiting for indexing to finish is not possible there, because the platform suspends the indexing queue for the whole duration of a sync, so the lookups now read up-to-date index data directly. The library classes deciding message-mapping and aspect support are also resolved once per sync rather than once per bean (#433)
- fix: Offer the event declaration as the publisher of any event published from a library, not only of the Boot and context lifecycle events: Spring Security raises `AuthenticationSuccessEvent`, Spring Data its keyspace and mapping events, and none of them inherit `SpringApplicationEvent` or `ApplicationContextEvent`, so their listeners still reported no publisher. An event now qualifies when it inherits `ApplicationEvent`, is declared in a library, and the project publishes no `publishEvent` call for it — the last condition, checked per event class, is what keeps the real call for an application event extending a library base and for a library event the application raises itself (#431)
- fix: Offer the event declaration as the publisher of a container-published lifecycle event, and say `No publisher found in project sources` instead of `No matching event publisher found` when nothing is found: `@EventListener(ApplicationReadyEvent.class)` and its siblings are fired by Spring from inside a jar, whose sources root is outside every module search scope, so a reference search can never reach that call and the popup claimed no publisher existed. Such an event is recognised by inheritance from `SpringApplicationEvent` or `ApplicationContextEvent` **and** by being declared in a library, so an application event that merely extends a lifecycle base still resolves to its own `publishEvent` call (#426)
- fix: Navigate from a `publishEvent(...)` call declared in a library to the listeners of that event, instead of answering "No matching event listener found": the gutter is installed on such a call — which is where Spring itself publishes every lifecycle event, e.g. `ApplicationReadyEvent` from `EventPublishingRunListener.ready` — but the search resolved no module for PSI inside a dependency and returned nothing. A call anchored in a library is now searched across the project rather than through the dependencies of whichever attached module the platform happened to pick, so a listener declared in a sibling module is found too (#427)
- fix: Name the navigation group of every Explyt gutter target, so `Navigate | Related Symbol` no longer lists Spring, AOP, SPI, configuration and Quarkus targets under a section header reading "XML": the single-argument `NavigationGutterIconBuilder.create(icon)` attaches the platform default group, which is the XML one, and that string is rendered as the popup separator (#411)
- fix: Present an event publisher in the "Navigate to event publisher" popup as the method that publishes it, with its file and line, instead of the raw source text of the whole `publishEvent(...)` call: the targets are call expressions, which have no name for the platform to show, so a multi-line publish call made the popup as wide as the call itself and left every row indistinguishable. The popup title now also names the event type being searched for (#410)
- fix: Fold a `@Value` placeholder and an `Environment.getProperty` key to the value defined in a production source root rather than one defined under `src/test/resources`: test configuration has been in scope since configuration files started being collected from dependencies, and a test `application.yaml` tied the profile-less ranking of its production namesake, so which of the two won was decided by index iteration order and could differ between sessions. A key defined only in test sources still folds
- fix: Name the class that is actually missing when a Native Context Mode sync fails with `NoClassDefFoundError`, instead of always blaming an unsupported Spring Boot version: the "only since 2.4.0" hint now appears solely for the Spring startup classes the bean reader needs, and any other absent class is reported together with the libraries on the launch classpath whose file was never downloaded
- fix: Make "Detach All Spring Boot Projects" enumerate the linked-project settings instead of the import-data cache: a project whose refresh never succeeded (or whose cached structure was dropped) was invisible to the action and stayed linked forever, and the debug-session link could never be detached at all
- fix: Report why "Detach All Spring Boot Projects" did nothing instead of discarding the failure: the reflective call into the platform's internal detach action swallowed every exception, so a signature change or a failed detach left no trace in the log at all. The failure is now logged, the number of linked projects is logged before the loop, and a missing platform method is named explicitly
- fix: Unlink an Explyt Spring project whose import data is absent — after a refresh that never succeeded, or once its cache was dropped, the detach found no project node and returned silently, leaving the link in the project settings forever. The link is now dropped through the external-system settings in that case
- fix: Refresh the Explyt Spring tool window after unlinking a single project, which only "Detach All Spring Boot Projects" did: the detached node stayed in the tree until the next full structure update
- fix: Heal or drop a Spring project link whose run configuration no longer exists, instead of failing every sync with "Run configuration '...' no longer exists": when exactly one run configuration points at the link's main-class file, the link is rebound and the sync continues in the same pass; a link with no candidate and no cached project data is removed, since it only produced an error on every refresh — links with cached data and configurations that fail at runtime are left untouched
- feat: Link a Gradle `bootRun` run configuration in Native Context Mode: selecting one no longer hides the Load Beans action, its module and main class resolve from the task prefix, and its environment variables and VM options travel into the launched application — previously only Java/Kotlin/Spring Boot configurations could be linked, so a project whose launcher is Gradle started without its profile and environment (#376)
- fix: Count a `@Value` placeholder in a dependent module — the shape of a Gradle test source-set module, which has no dependents of its own — as a usage of a configuration key: configuration files are now collected from both dependents and dependencies, so the main `application.yaml` no longer reports such a key as unresolved and the test-source placeholder navigates to the main definition (#381)
- fix: Count a `${...}` placeholder in any `org.springframework.*` annotation attribute as a usage of the configuration key, not only in `@Value` and `@Scheduled`: a key consumed through `@KafkaListener(topics = ["\${...}"])`, `@RequestMapping(path = ...)`, `@RabbitListener` and the like no longer reports `Cannot resolve key property`, and navigates and renames like a `@Value` usage (#380)
- fix: Resolve `management.endpoint.<id>.access`, `.enabled` and `.cache.time-to-live` of an Actuator endpoint declared in a dependency module — endpoint discovery searched only the configuration module's own sources, so a shared starter's endpoint was reported as an unresolved key and had no navigation (#382)
- fix: Count a configuration key used through `@ConditionalOnProperty`, `Environment.getProperty` or `DynamicPropertyRegistry` as referenced when it is defined in several configuration files: the reference's single-target resolve returned nothing for two or more definitions, so every file defining the key reported "Cannot resolve key property" (#118)
- fix: Do not report a configuration value as invalid when the metadata hint that declares its closed value set allows other values: the check was gated on the `<prefix>.keys` hint while validating against `<prefix>.values`, so `logging.level.root=INFO` was an error even though the `logging.level.values` hint declares provider `any` — and a property with only a `.keys` hint was told its value `must be one of []` (#385)
- feat: Link a Gradle `bootRun` run configuration in Native Context Mode: selecting one no longer hides the Load Beans action, its module and main class resolve from the task prefix, and its environment variables and VM options travel into the launched application — previously only Java/Kotlin/Spring Boot configurations could be linked, so a project whose launcher is Gradle started without its profile and environment (#376)
- fix: Navigate from a list-element key nested under a map entry, so `app.publishers."[my.registration]".routes[0].payload-type` in properties and its index-less YAML form resolve to the element type's member: map-value members are not declared as configuration properties, so the key remainder is now walked level by level — map value type, its list property, the element type (#396)
- fix: Navigate from a map-entry or list-element member key to a constructor-bound property — a Kotlin `val` in a data class or a Java record component — instead of only to JavaBean setters, which constructor-bound classes do not have (#384)
- fix: Navigate from a map-entry key written in Spring's bracket notation, so `app.publishers."[my.registration]".owner-application` in YAML and `app.publishers[my.registration].owner-application` in properties resolve to the map declaration and the declaring member instead of breaking on the dots inside the brackets (#383)
- fix: Report a non-canonical configuration key on the segment that actually deviates instead of on the deepest key, so `explyt.camel.camelWritten.items[0].name` underlines `camelWritten` rather than the perfectly canonical `name`, and several keys under one such ancestor report it once instead of once each
- fix: Offer one navigation target for a configuration key declared by a library, instead of the same declaration once from the jar and once from its sources jar: the two were folded together by rewriting the file name while keeping the path, and Gradle caches them under two different checksum directories
- fix: Inspect a configuration key whose value is a YAML sequence. Only a scalar-valued key was collected, so a list-valued one such as `paths_to_exclude:` was skipped by every per-key check — canonical form, unresolved key, deprecation and the profile-file restriction. A sequence has no scalar value, so the value-level checks correctly report nothing for it
- fix: Report a configuration key that is not in Spring's canonical form as a weak warning rather than a warning, and say so in those words: relaxed binding resolves `myKey`, `my-key` and `my_key` to the same property, so the spelling is a style deviation and does not belong next to "Cannot resolve key property". The same applies to a `@ConfigurationProperties` prefix
- fix: Show a plain apostrophe in the four messages that rendered a doubled one — "Don''t show again", the `javax`/`jakarta` import fix, the `@Async` return type warning and the recommended-spelling inspection name. `MessageFormat` collapses `''` only when a message is rendered with arguments, and these four take none
- fix: Resolve the clicked main-class file once when the Load Beans gutter icon links a project, instead of asking every run configuration for its resolved main class — which made the Spring Boot configuration search for a main class candidate through the indexes and jar attributes, once per configuration, on the event dispatch thread
- fix: Link the Spring Boot project from the gutter icon to the run configuration that actually matches the main class, instead of returning the selected one — which the check above had already rejected, and which is null when nothing is selected
- fix: Do not fail the "Switch to kebab-case" quick fix with `IncorrectOperationException`: every `${...}` usage of the renamed key was rebuilt through a Java expression parser, which cannot parse a bare key, a kebab-cased one (the dashes read as subtraction), a segment that is a Java keyword such as `default`, or a nested placeholder chain. Usages are now rewritten in the document, which also preserves the quoting style and the `:default` suffix
- fix: Apply that quick fix in batch mode too (`Fix all`, `Code | Inspect Code`): its whole body was guarded by `if (editor != null)`, so a batch run reported success and changed nothing
- fix: Follow Spring's own `ConventionUtils.toDashedCase` when converting a configuration key to kebab-case. A dash belongs before an uppercase letter and in place of `-`/`_`, never at a digit boundary, so `v4` stays `v4` instead of becoming `v-4` while `s3Logs` still becomes `s3-logs`
- fix: Do not freeze the UI when a run configuration is selected: reading the active profiles asked the run configuration for its run class, which makes the Spring Boot configuration search for a main class candidate through indexes and jar attributes — on the event dispatch thread. The stored main class name is read instead, which is all the caller compares
- fix: Let the Beans tab of Search Everywhere be interrupted by a write action instead of holding a read action until the whole bean model is built, which delayed every concurrent edit
- fix: Keep the one-time feedback nudge on screen until it is acted on, show it right after the engagement threshold is crossed instead of during IDE startup, and let installs that predate the nudge qualify from their existing usage instead of starting from zero
- fix: Persist the local usage counters across IDE restarts — they were updated through `Map.compute`, which the platform's stored map does not count as a modification, so the state was never saved; they now live in a local, non-synced options file instead of the cache-file storage, which is saved at most once every five minutes, never forced on exit, and on 2026.2 kept in the opaque internal settings database
- fix: Fold a `@Value` placeholder and an `Environment.getProperty` key to the value of the profile-less `application.*` file instead of an arbitrary one, and name the profile when the value comes only from `application-<profile>.*`
- fix: Cache the `@PropertySource` lookup that decides whether a file is Spring configuration, so highlighting an unrelated `.properties`/`.yaml` file no longer runs a project-wide index search per PSI element
- fix: Look up a configuration key in the metadata catalogue through an index instead of scanning it: validating a `.properties`/`.yaml` file re-normalised every one of the thousands of catalogue names for every key in the file, on every highlighting pass
- fix: Look up metadata hints by name instead of walking the whole hint list once per key per check
- fix: Stop resolving every call in the file to decide whether it looks up a resource: the resource-reference inspection now rejects a call by its argument count first, so highlighting no longer walks into unrelated library PSI on calls it was always going to ignore
- fix: Report the `BootstrapRegistry`, `@JsonComponent`/`@JsonMixin` and `@EntityScan` migrations when the legacy symbol no longer resolves after a Spring Boot 4 upgrade
- fix: Report the `@MockBean` / `@SpyBean` migration when the legacy annotation no longer resolves after a Spring Boot 4 upgrade
- feat: Report the `@MockBean` / `@SpyBean` migration already in Spring Boot 3.4/3.5, where the annotations are deprecated for removal, naming `@MockitoBean` / `@MockitoSpyBean` and offering the quick-fix the platform deprecation warning cannot
- feat: Report the renamed `management.endpoint.httptrace.*` configuration keys in Spring Boot 3, with a quick-fix to `httpexchanges`
- fix: Navigate from an indexed collection key such as `ingest.s3-logs.sources[0].enabled` to the member of the collection's element class
- fix: Resolve a configuration value against the enum element type of a collection property such as `java.util.Set<...>`
- fix: Accept an enum configuration value in any relaxed form, so `request-headers` resolves to `REQUEST_HEADERS` and is no longer reported as invalid
- fix: Complete an enum configuration value from any relaxed spelling and insert Spring's recommended one, so `r`, `request-h`, `request_h` and `REQ` all insert `request-headers`
- fix: Complete a metadata hint value from a prefix written in any case and insert the declared literal, so `logging.level.org.springframework=IN` offers and inserts `info`
- feat: Report an enum configuration value that is not written in Spring's recommended spelling, with a quick-fix rewriting `REQUEST_HEADERS` to `request-headers`
- fix: Resolve a configuration value against the enum element type of an array property such as `Include[]` and of a map property such as `java.util.Map<String, Include>`
- fix: Navigate a configuration value to its metadata hint declaration whatever its case, so `logging.level.root=INFO` navigates like `info`
- feat: Report a configuration value that differs from its metadata hint literal only by case, with a quick-fix rewriting `INFO` to `info`
- fix: Offer one navigation target per metadata declaration instead of repeating the same hint or key once per metadata file and once per sources jar of the same artifact
- fix: Navigate from the key of `logging.level.<suffix>` to what the suffix names: the package or class of a logger name such as `org.springframework`, the `logging.level.keys` hint declaration of a group such as `root`, `sql` or `web`, and the `logging.group.<name>` entry of a group the project defines itself
- fix: Navigate a configuration key with no declaring member, such as `management.endpoint.httpexchanges.access`, to its value type instead of the unrelated source class
- fix: Stop reporting `management.endpoint.<id>.access`, `.enabled` and `.cache.time-to-live` of an Actuator endpoint the project declares itself as an unresolved key, and complete and navigate them: the id segment leads to the endpoint class, the value segment to the type it takes (#314)
- fix: Do not fail the Alt+Enter preview of the deprecated configuration key replacement quick-fix (#296)
- fix: Do not report `@Autowired` members of `@ContextConfiguration` test classes as not being a Spring bean
- fix: Evaluate the IntelliJ IDEA Ultimate line marker suppression once per highlighting batch instead of once per PSI element
- fix: Do not freeze the UI while a project opens: the linked-project self-healing pass no longer requests the run configuration manager on the event dispatch thread (#294)
- fix: Stop resolving PSI in the Spring Boot toolbar action update, which caused multi-second action-update delays
- fix: Keep the Spring Boot toolbar button responsive by not creating the run configuration manager while the action updates
- fix: Do not show the Spring Boot debug value hint in an editor that has no file behind it (#186)
- fix: Ignore a cached bean whose PSI is no longer valid instead of failing inspections, gutters and bean search (#295)
- fix: Apply that same filter on the source bean-search path, not only the native one, so a bean invalidated by an edit no longer breaks the autowiring inspection and bean navigation in a project without an external Spring model (#205)
- fix: Reflect an edited source file in reference search results instead of returning a stale cached set
- fix: Read the main class of a Kotlin run configuration without resolving PSI on the event dispatch thread (#185)
- fix: Resolve a configuration key with no exact declaration to the longest declared prefix that owns it, instead of whichever prefix the metadata catalogue happened to list first
- fix: Skip a cached component annotation that an edit invalidated instead of passing it to the annotated-elements search, which failed the whole bean search on it — with an `IllegalArgumentException` from the Java search executor and a message-less `AssertionError` from the Groovy one
- fix: Recompute the bean search instead of failing it when a search executor dereferences an element invalidated while the query ran
- feat: Open the Explyt Spring tool window after linking a Spring Boot project from a run configuration, including when the project was already linked and the click only refreshes it (#197)
- feat: Navigate from a SpEL bean reference in `@Value` and `@Scheduled` to the bean and to the property it reads, so `#{@myProps.cron}` resolves both halves and completes the property name (#44)
- fix: Require a configuration key's owning declaration to end at a segment boundary, so a declared `foo.bar` no longer owns the unrelated `foo.barbaz` — it decided the value type, the map-entry completion, the kebab-case exemption and whether an unknown key was reported at all
- fix: support spring boot4 test annotations MockitoSpyBean/MockitoBean (#418)

### Spring Initializr
- fix: Make `gradlew` and `mvnw` executable in a project generated through Spring Initializr, so the first `./gradlew` in a terminal no longer fails with "permission denied" (#60)

### Spring Web
- fix: Load the JSON schema validator without holding the schema cache lock: the first specification opened paid the validator's class initialisation while every other thread asking for the same schema waited behind it
- fix: Let the OAuth2 Authorize flow of the OpenAPI preview complete. Swagger UI was left to derive its own redirect URL, and since the preview is served from `/explyt-openapi?key=…&resource=index.html` — a path whose query string is not part of `location.pathname` — that derivation produced `/oauth2-redirect.html` at the server root, which the preview does not serve. The redirect is now a stable, key-less endpoint the specification's provider can be registered against, served by its own handler that accepts the authorization server as the origin of the redirect while still refusing any request not addressed to the loopback interface
- feat: Recognise OpenAPI 3.2 specifications and validate them against the bundled `3.2/schema/2025-11-23`: an `openapi: 3.2.x` document was classified as undefined, which left it with no schema, no completion, no specification icon and no validation. The version is also offered by `openapi:` completion and survives a restart when picked manually for a file
- fix: Refresh the bundled OpenAPI 3.0 and 3.1 schemas, three and four years stale respectively — 3.0 moves `2021-09-28` → `2024-10-18`, which splits `Parameter` into the four location-specific variants, and 3.1 moves `2022-10-07` → `2025-11-23`, which revises 29 of its 40 definitions
- fix: Point the schema remote source at the versioned `spec.openapis.org` URL the bundled copy is taken from; both `raw.githubusercontent.com` links answered 404, so the IDE could never fetch the upstream schema behind the embedded one
- fix: Update the bundled Swagger UI used by the specification preview to 5.32.15, and with it `swagger-ui-standalone-preset.js.map`, which held a copy of the stylesheet rather than a source map
- chore: Drop `swagger-ui-bundle.js.map` and `swagger-ui-standalone-preset.js.map` from the plugin, halving the vendored preview assets from 4.34 MB to 2.20 MB: neither bundle carries a `sourceMappingURL`, upstream does not add one, so nothing could ever load them. `swagger-ui.css.map`, which the stylesheet does reference, is kept
- chore: Ship the `swagger-ui-bundle.js.LICENSE.txt` and `swagger-ui-standalone-preset.js.LICENSE.txt` sidecars, which carry the third-party notices webpack strips out of the minified bundles
- feat: List Actuator endpoints in the Endpoints tool window, one row per `@ReadOperation`/`@WriteOperation`/`@DeleteOperation` with its `@Selector` path variables, and navigate to the declaring class and the operation method; the path follows `management.endpoints.web.base-path`, `management.endpoints.web.path-mapping.<id>` and `management.server.port` (#315)
- fix: Register the web additional-beans discoverer under the extension namespace that declares the extension point, so framework-provided web beans such as `WebApplicationContext` are resolved again
- fix: Resolve `MockMvc` and `WebTestClient` autowired in tests as beans provided by test auto-configuration
- fix: Inject the regular expression of a `@RequestMapping` path built by concatenation into the literal that actually contains it

### Spring MCP
- feat: Answer `explyt_get_spring_data_entities` with a bounded, paginated inventory instead of the full schema of every entity at once — a **breaking change** to its response, which is now `{status, revision, totalCount, offset, truncated, nextOffset, entities}` rather than a bare array. By default an entity carries only its name, class, table and source location, so an agent can pick one without receiving the fields, relationships and indexes of the whole project first; `includeDetails=true` adds that schema, and `className` selects one entity by its exact FQN. An inventory record omits `fields` and `indexes` entirely rather than emitting them empty, so "not requested" cannot be read as "this entity declares none". The silent 500-entity cutoff is gone: every entity stays reachable by repeating the call with `offset` = `nextOffset` and `expectedRevision` = `revision`, and the budget is measured on the finished JSON rather than on a number of entities, so a single entity too large for it is reported as `RESPONSE_TOO_LARGE` naming the `maxChars` that would fetch it instead of being truncated or dropped
- fix: Refuse a `projectPath` that names no open project, instead of answering it from whichever project happens to be open: with a single project open the argument was ignored entirely, so a mistyped path, a path of a project that is not open, or one left over from an earlier session produced a confident answer about a different codebase that a caller cannot tell from a correct one. The path is now matched after normalisation, so a trailing separator or a `..` segment still names its project, and `projectPath` became optional across every tool — omitting it answers from the single open project, and with several open the tools ask for one by name rather than picking. `explyt_get_project_beans_by_spring_boot_application` no longer falls back to searching other open projects for the application class when a path was supplied
- fix: Report every handler parameter in `explyt_get_spring_endpoint_contract` and `explyt_get_spring_http_endpoints`, not only the annotation-bound ones: a parameter bound by a custom `HandlerMethodArgumentResolver` was dropped silently, which is indistinguishable from an endpoint that never declared it — the opposite conclusion when the parameter is the one carrying authorization. Each parameter now names its `source` (`PATH`, `QUERY`, `BODY`, `HEADER`, `COOKIE`, `MODEL`, `FRAMEWORK` or `UNKNOWN`), and `required` is null wherever nothing declares it
- feat: Add `compact` to `explyt_get_spring_http_endpoints`, omitting the `parameters` and `returnType` of each endpoint — they dominate the response, which on a 136-endpoint project reached 122 KB of single-line JSON that a line-based reader cannot chunk, and the controller/type filters cannot narrow it in the situation the tool is first called for: not yet knowing which controllers exist
- fix: Describe the endpoint listing with the field names it actually returns, and show one complete example object: the description said "HTTP method, full path, line number" while the keys are `httpMethods` (an array), `fullPath` and `line`, so a first parse written against the prose silently yielded empty rows instead of failing
- fix: Lead every MCP tool description with the moment to call the tool and the mistake it prevents, instead of the capability it offers. An agent extending a controller read "Finds endpoints matching a URL pattern" as a lookup and reasoned about the composed path and sixteen query parameters by hand, calling the tool only at the end — where it confirmed everything in one call. `explyt_find_spring_endpoint` now says to call it after adding a mapping and what an empty result means; `explyt_get_spring_http_endpoints` opens with "one controller (`controllerFilter`) or the whole project" rather than "all HTTP endpoints in the project", which read as expensive and was skipped
- feat: Declare every Spring MCP tool read-only and idempotent through the MCP tool annotations, and give each a title. A client uses these hints to skip the per-call confirmation it asks for an unclassified tool
- feat: Answer a miss in `explyt_find_spring_endpoint` and `explyt_get_spring_endpoint_contract` with the routes around it. Both tools now return `{totalCount, truncated, endpoints, sharedPrefix, nearestByPrefix}` instead of a bare array: `endpoints` is ordered the way Spring picks a handler — a literal route before a `{template}` one, fewer wildcards first — so when several match one URL the first is the one that dispatches; when nothing matches, `nearestByPrefix` lists the existing routes sharing the longest leading path with the pattern, which is the controller a new route belongs to, and `truncated` replaces the cap of 50 matches that was applied silently. A client parsing the old array has to read `endpoints` now
- feat: Add `explyt_find_spring_bean`, answering one bean question about one explicitly chosen application: which beans a type or an exact name (including a `@Bean` alias) resolves to, or what Spring would inject at one constructor parameter, field or setter given as `filePath` and `line`. The answer is relative to the model that produced it and says so — a loaded application context when one is available, otherwise an estimate of the module, named as such — and never claims the application would start. A Kotlin parameter with a default value reports `required` and `hasDefaultValue` as facts of the declaration, independent of whether a candidate exists, so "no bean, and none needed" is distinguishable from "no bean, and one is required". The response is bounded to 1800 characters by default and paged, with `revision` tied to both the model and the query so a continuation cannot silently describe a different answer
- fix: Answer `explyt_get_project_beans_by_spring_boot_application` from the same chosen application model the bean lookup uses, instead of the module's stereotype scan. A `@Bean` whose return type comes from a library — `@Bean Clock` being the ordinary case — was dropped entirely, because the declaring module was taken from the bean's type, and `java.time.Clock` belongs to no project module; it is now taken from the factory method that declares it. A bean a loaded context reports without project sources keeps its row with an empty `moduleName` rather than disappearing, and a context that cannot be chosen is reported as a failure naming what to disambiguate instead of an empty array. The successful array schema is unchanged, and the optional `source` and `contextId` arguments select the model explicitly

### Other
- fix: Do not freeze the UI on the first action after the IDE starts: error-reporting setup no longer runs while an action is being recorded (#307)
- fix: Keep Sentry action breadcrumbs useful by dropping editor typing and caret movement, recording a repeated action once, and trimming a captured report to the actions that led to it
- fix: Bring error reporting back up when the pooled executor rejects the initialization task, instead of leaving it disabled for the rest of the session
- docs: Correct the contributor quickstart, which named the wrong sandbox IDE and JDK: since 2025.3 IntelliJ IDEA is a single unified distribution, so `253` and newer build against it rather than against Community
- chore: Drop the `defaultIdeaType` property and its sixteen unused declarations, inert since the platform dependency stopped taking an edition argument on `253`

## [262.34.101] - 2026-08-19

### Spring Core
- fix: Keep renamed Spring Boot run configurations linked without ambiguous fallback (#229) (#279)
- fix: Keep Kotlin top-level `main()` projects linked when the stored run configuration name no longer exists (#229)
- fix: Restore Spring project links when a run configuration is renamed or replaced outside the IDE, such as by a version-control update (#229)
- fix: Hide the Beans Search Everywhere tab outside Spring Boot projects without blocking EDT (#233) (#240) (#279)
- fix: Anchor inherited autowired inspection problems (#234) (#241)
- fix: Prevent cyclic inherited-autowiring traversal from hanging inspection analysis (#279)
- fix: Avoid invalid TextRange in `@Value` reference provider (#236) (#238)
- fix: Handle transient PSI file text mismatch in bean search (#232) (#243)
- fix: Do not require `@ConstructorBinding` in Spring Boot 3 (#252)
- feat: Add `Copy Full Property Path` and `Copy as Environment Variable` actions for properties/YAML (#266)
- feat: Hide duplicate Explyt line markers and Related Symbol targets when IntelliJ IDEA Ultimate provides Spring support (#269) (#279)
- feat: Add Spring Boot 3 configuration property migration inspection (#254)
- feat: Add an in-IDE feedback nudge for engaged users (#250)
- fix: Validate literal collection elements alongside unresolved placeholders (#247) (#279)
- feat: Migrate Actuator `httptrace` exposure to `httpexchanges` in Spring Boot 3 (#256)
- fix: Avoid EDT work while searching Spring beans (#249)
- feat: Add Spring Boot 4 configuration property migration inspection (#257)
- feat: Migrate Actuator endpoint `@Nullable` to JSpecify in Spring Boot 4 (#264)
- feat: Warn when a Spring bean injected into a Spring Boot 3 `@ConfigurationProperties` constructor lacks `@Autowired` (#255)
- feat: Migrate `@MockBean` and `@SpyBean` to `@MockitoBean` and `@MockitoSpyBean` in Spring Boot 4 (#258) (#270)
- feat: Migrate `BootstrapRegistry` and `BootstrapContext` imports for Spring Boot 4 (#261)
- feat: Warn when `@SpringBootTest` lacks Web Test Client auto-configuration in Spring Boot 4 (#260)
- feat: Migrate `@EntityScan` import for Spring Boot 4 (#259)
- feat: Migrate `TestRestTemplate` import for Spring Boot 4 (#262)
- feat: Migrate `@JsonComponent` and `@JsonMixin` to `@JacksonComponent` and `@JacksonMixin` in Spring Boot 4 (#263)
- fix: Close the quoted placeholder in the Spring Boot 4 property migration message (#272)
- feat: Add Jakarta namespace migration inspection (`javax` to `jakarta`) (#253)
- feat: Warn on Jackson 2 `ObjectMapper` injection in Spring Boot 4 (#246) (#273)
- fix: Harden Spring Boot 3/4 migration inspections and quick-fixes for Java, Kotlin, unresolved imports, YAML sequences, and module scopes (#279)
- fix: Resolve list/map element property keys below digit-boundary names such as `s3Logs` (#271)
- fix: Resolve Kotlin `const val` package names in component-scan annotations
- fix: Resolve `@Value` placeholder keys whose default is a SpEL or nested placeholder expression, such as `${my.key:#{null}}`
- fix: Resolve property keys consumed only from a dependency module (#276)

### Spring Data
- fix: Fail closed on stale PSI in SQL injector (#235) (#242)

### Spring Web
- fix: EDT while navigation (#227)
- fix: Load HTTP environment JSON PSI asynchronously off EDT (#237) (#239) (#279)

### Spring MCP
- fix: Paginate `explyt_get_spring_http_endpoints` results and preserve total-count metadata (#244) (#279)
- fix: Keep MCP tools working on Kotlin light and synthetic PSI without a source range, omitting `line` when no declaration can be pointed at (#281)

### Other
- ci: Pin the Sentry release action and restrict release-job permissions (#230) (#279)
- ci: Ignore PLUGIN_STRUCTURE_WARNINGS
- chore: Configure Plugin Verifier
- chore: Finish migrating shipped resources to Apache-2.0 (#208) (#279)
- docs: Update README and plugin description
- docs: Update issue/PR templates and contributing guide
- docs: Add AI Agent documentation
- chore: Support IntelliJ Platform 2026.2 (262)
- fix: Replace internal plugin-manager APIs with `PluginDetailsService` to unblock 2026.2 plugin verification

## 261.33.80 - 2026-04-28

### Spring Core
- feat: 261 spring boot support (#217)
- feat: Spring Mock/Spy beans in test #209 (#211)
- fix: Redundant line marker under `@Value` constructor (#210)

### Spring Initializr
- fix: Spring Init (#218)

### Spring MCP
- feat: MCP Tool `explyt_get_spring_data_entities` (#222)
- feat: MCP Tools `explyt_get_spring_http_endpoints` and `explyt_get_spring_endpoint_contract` (#221)
- feat: MCP Tool `explyt_trace_spring_call_chain` (#220)
- feat: MCP Tool `explyt_find_spring_endpoint` (#219)

### Other
- fix: EDT #213 (#214)
- chore: Support 261 (#192)
- chore: Prepare release 33
- chore: Migrate Sentry (#215)
- chore: Add tests with TypeChecks
- chore: Update plugin versions in GitHub Workflows
- chore: Add information about MCP into README files
- chore: Fix outdated URL
- docs: Add tags to the urls

## 253.32.71 - 2026-01-31

### Spring Core

- feat: Show EventListeners in endpoint tool
  window [#181](https://github.com/explyt/spring-plugin/issues/181#issuecomment-4021542501)
- feat: Support method beans from @Import annotation [#189](https://github.com/explyt/spring-plugin/issues/189)
- fix: support ResourceLoader bean [#190](https://github.com/explyt/spring-plugin/issues/190)
- fix: StackOverflowError in ExplytPsiUtil [#196](https://github.com/explyt/spring-plugin/issues/196)
- fix: Error during ranges calculation in yaml annotator [#173](https://github.com/explyt/spring-plugin/issues/173)

### Spring Test

- fix: Test beans inspections [#183](https://github.com/explyt/spring-plugin/issues/183)

### Spring Core/Boot

- feat: Kotlin: Quick-fix for data class @ConfigurationProperties without
  `@ConstructorBinding` [#31](https://github.com/explyt/spring-plugin/issues/31)

## 253.31.58 - 2026-01-31

### Spring AI

- feat: Spring AI Tools for bundled [MCP Server plugin](https://plugins.jetbrains.com/plugin/26071-mcp-server)

### Spring Core/Boot

- feat: Kotlin: Quick-fix for data class @ConfigurationProperties without `@ConstructorBinding` [#31](https://github.com/explyt/spring-plugin/issues/31)
- fix: Slow operation in EDT [#154](https://github.com/explyt/spring-plugin/issues/154)
- fix: ConfigurationProperties for kotlin inspection [#163](https://github.com/explyt/spring-plugin/issues/163)
- fix: PathVariable in Class URL [#161](https://github.com/explyt/spring-plugin/issues/161)
- fix: Events for @TransactionalEventListener [PR](https://github.com/explyt/spring-plugin/pull/179)
- fix: Bean Autowired gutter icon for lombok

## 253.30.53 - 2025-12-22

### Spring Core/Boot

- feat: [Endpoints](https://github.com/explyt/spring-plugin/pull/143#issuecomment-3641799841) support for message brokers and Spring Boot (demo video)
- feat: Added Spring Aspect nodes to the Explyt Spring
  [Tool Window](https://github.com/explyt/spring-plugin/pull/151#issuecomment-3641786335) (demo video)
- feat: Properties converter between `.yml` and `.properties`. [#153](https://github.com/explyt/spring-plugin/pull/153)
- fix: `@ConfigurationProperties` quick-fix improvements. [details](https://github.com/explyt/spring-plugin/pull/153#issuecomment-3664193934) (demo video)
- fix: Redundant property inspection and gutter icon behavior. [Issue](https://github.com/explyt/spring-plugin/issues/114)
- fix: Kotlin template string inspection for `@PathVariable`. [Issue](https://github.com/explyt/spring-plugin/issues/142)
- fix: YAML kebab-case quick fix. [Issue](https://github.com/explyt/spring-plugin/issues/147)

### Spring Data

- feat: Spring Data 4.0 [AOT navigation](https://github.com/explyt/spring-plugin/pull/148#issuecomment-3641770937) from
  repository methods (demo video).

### Spring Debugger

- feat: [Support](https://storage.yandexcloud.net/explyt-web/videos/spring-wiki/debugger/spring-debug-old.mp4) for IntelliJ IDEA versions 241 / 242 (demo video).

## 252.29.43 - 2025-11-24

### Spring Core/Boot

- fix: load beans by -javaagent on [Windows](https://github.com/explyt/spring-plugin/issues/125)
- feat: multi context [support](https://github.com/explyt/spring-plugin/issues/116)

### Docker

- fix: improve 'env' variables [completions](https://github.com/explyt/spring-plugin/issues/119)
- feat: support 'env' completions in k8s.yml files

### Spring Web

- fix: [improve](https://github.com/explyt/spring-plugin/issues/115) @PathVariable support. Regexp & QuickFixes

## 252.28.37 - 2025-11-05

### Spring Core/Boot
- fix: configuration properties field should not be autowired

### Spring Initialzr
- fix: download indicator exception

### Spring Debugger
- feat: Spring Remote Debugger

### Docker 
- feat: env variables completion in docker compose files

### SQL
- feat: introduce basic SQL DML support (no plugin required)

### Other
- chore: Update plugin name to Spring Explyt
- fix: Remove internal API usages and access to FUS 

## 252.27.26 - 2025-10-02

### Spring Core/Boot
- fix: log pattern error & additional bean search
- fix: Race error in case if several inspections were started at once

### Spring AI
- feat: AI add action Entity to DB Script

### Spring Data
- feat: JDBC client inspections (#68)

### Spring Debugger
- feat: Spring Debugger runtime property - code vision
- feat: Spring Debugger show server Web URL in console
- chore: Update debugger hint icon

## 252.26.18 - 2025-09-18

### Spring Core/Boot
- feat: Support Spring 7: Bean Registrar

### Spring AI
- fix: ai prompts

### Spring Data
- feat: JDBC client params navigation & inspections

## 252.25.15 - 2025-09-05

### Spring AI 
- feat: Explyt AI integration

### Spring Core/Boot
- feat: library beans DI support
- feat: Scheduled cron's zone attribute support 
- fix: Property definition inspection 
- fix: property cache (#74)
- fix: ConditionalOnMissingBeanStrategy - method from the same class (#76)

### Spring Data
- JPA generate equals & hashCode methods

### Spring Web / OpenApi
- fix: Spring web parse url

### Spring Debugger
- Improve spring debugger node

## 252.24.11 - 2025-08-15

### Spring Core/Boot
- Add an icon to the "Mark directory as Spring Configuration Root" action
- fix: Maps with string key/value in properties wasn't resolved
- fix: settings ref for agent mode #59
- fix: various EDT issues

### Spring Web / OpenApi
- fix: Use the correct server port

### HttpClient
- feat: HTTP completion in .http files

### Spring Debugger
- feat: revert gradle configuration after debug
- fix: remove setStoreExternally for debugger
- fix: Debugger run configuration - empty map is immutable

### Other
- fix: 252 only! remove same warnings (#61)

## 252.23.8 - 2025-08-05

### Spring Core/Boot
- feat: Spring Debugger
- feat: Mark/unmark folder as Spring Configuration Root 
- fix: Invalid references when there are comment and placeholder arguments (#47)
- fix: Only first object referenced without warning in properties for fields with the same type in @ConfigurationProperties
- fix: Various bug fixes

### Spring Web / OpenApi
- feat: Support latest OpenApi in swagger-ui (#50)

### Spring Data
- feat: Mark some strings as SQL (#54)

### Other
- fix: Error reporting issues
- fix: Invalid injection host exception (#51)
- chore: support 252

## 251.22.6 - 2025-06-09

### Quarkus
- feat: Detect Quarkus endpoints in the Endpoint panel
- fix: Various bug fixes

### Spring Core/Boot
- fix: Minor fixes

### Other
- feat: Add Spring tool for the Explyt AI platform
- fix: GlitchTip error reporting

## 251.1.15986533443 - 2025-07-01

### Quarkus
- Feature: Support Quarkus DI (#34)

### Spring Core/Boot
- feat: application.yaml enum key error (#32)

### Spring Initializr
- fix: since 2025.1 (251) DownloadHandler in JCefBrowser does not work (#35)
- chore: display verbose errors while Spring Project creation

### Other
- feat: 2025.2 (252) initial support
- chore: migrate from sentry to glichtip

## 251.1.3531 - 2025-04-17

### Spring Core/Boot
- Added: SPI bean navigation support (#29)
- Added: Support properties mapped to Map<Enum, V> (#28)
- Fixed: generic bean navigation for beans with java fragments
- Fixed: escape command line to start javaagent

### Spring Web / OpenApi
- Added: add to an openapi file all possible servers and ports from configs
- Fixed: optimize endpoints search
- Fixed: optimize editing of temporal openapi files

### Other
- HttpClient: added variable and line comment support in http files. (#26)
- Improve test build

## 243.1.3438 - 2025-03-25

### Spring Core/Boot
- Added: migrate agent to declarative-bytecode-patcher

### Other
- Fixed: various bugfixes

## 243.1.3391 - 2025-03-14

### Spring Core/Boot
- Added: use -javaagent for getting spring context

### Http Client
- Added: Baseline HTTP parser + lexer + syntax highlighter. (#23)

### Spring Data
- Fixed: sql was tried to be injected in invalid places 

## 243.1.3351 - 2025-02-20

### Spring Data
- Added: Support inject sql to `sql` named parameter functions (#16) 

## 243.1.3333 - 2025-02-11

### Spring Core/Boot

- Added: rename property for @ConfigurationProperties class methods (java) or constructor parameters (kotlin)
- Added: completion for Hibernate settings in configuration property
- Added: in properties/yaml completion for map value
- Fixed: rename property name in properties/yaml files and @Value
- Fixed: configuration: show comment for property from field in case of missing setter method
- Fixed: configuration: no property linemarkers for java record `@ConfigurationProperties`

### Spring Web / OpenAPI

- Added: Retrofit run linemarker (spec generation)
- Added: Retrofit endpoint actions
- Fixed: Endpoint's RunLinemarker is always available, removed `Run in Swagger` action from endpoint's actions
- Added: JAX-RS run linemarker (spec generation)
- Added: JAX-RS endpoint actions
- Added: Endpoint Tool Window shows JAX-RS endpoints
- Fixed: Openapi spec generation supports MultipartFile usage
- Added: HttpExchange endpoint actions
- Added: Endpoint Tool Window shows HttpExchange endpoints
- Fixed: `Generate OpenApi Doc` intention - create dir 'resources' if it does not exist
- Added Inspection: `Unknown reference` for openapi `$ref` (yaml, json)

### Other

- Added: Spring Boot Panel: Generate special starter code to connect the panel to scan project beans for complex projects
- Added: Support 'http' and 'rest' file extensions
- Added: Ability to specify cli runner (like JetBrains HttpClient or HttpYac) to execute http-scripts and other RFC 2616 scripts
- Added: Linemarkers in 'http' and 'rest' to execute statement

## 243.1.3155 - 2025-01-17

### Spring Core/Boot

- Fixed: Properties: quick-fix to kebab-case didn't work
- Fixed: Show correct linemarkers in library files with an enabled Spring Boot panel. 

### Spring Web / OpenAPI

- Added: Swagger is able to download a picture or binary file
- Fixed: Navigation from endpoint to MockMvc usage (kotlin)
- Chore: Now swagger uses JCEF browser through HttpClient
- Fixed: Navigation between MockMvc urls for a new version of MockMvc library
- Added: Use localhost as default server if server wasn't set in an OpenAPI file

### Other

- Added: Kotlin Run Configuration can be linked to Explyt Spring Boot Panel
- Added: Show progress while collecting project's endpoints in the Spring Endpoints panel 
- Added: Sync project with the Explyt Spring Boot panel using linemarker on @SpringBootApplication

## 243.1.3083 - 2024-12-29

### Spring Core/Boot

- Added: support for syntax highlighting in application.yaml and application.properties
- Added: completion/reference in property starting with logging.level
- Fixed: inspection kebab-case false positive for logging level property
- Fixed: fix inspection resource in tests (#15)

### Spring Web / OpenAPI

- Added: Action `Generate OpenApi Specification` for project
- Added: Run in Swagger action for Controller/FeignClient
- Added: Generate requestBody as custom type schema for Run controller/endpoint action
- Added: Another design for Spring Endpoints, new Line Marker icons
- Added: Generation of Http Client and OpenApi spec using cURL

### Other

- Added: Auto Configurations and Message Brokers folders to Spring Explyt View
- Fixed: stack overflow on jpql file find in EntityAttributeSearcher

## 243.1.2927 - 2024-12-16

### Spring Core/Boot

- Added: completion in property for field with type Resource the class with @ConfigurationProperties
- Fixed: support in property for boolean starting with "is"
- Fixed: reference in property Relaxed Binding
- Fixed: reference in property for Map values
- Fixed: include in search for class-reference not only compile dependencies, but runtime as well
- Fixed: The ExplytSpringBoot settings lead to the incorrect direction (#13)
- Fixed: inspection for fields and methods super classes (#14)

### Spring Web / OpenAPI

- Fixed: Removed navigation from endpoint to openapi path
- Added: Run in Swagger endpoint action
- Added: Generate OpenAPI from coRouter function (#10)
- Added: Floating button for refresh ExplytSpringBoot panel
- Added: Generate Spring MVC controller method by url in Generate intention

## 243.1.2801 - 2024-12-01

### Spring Core/Boot

- Added: Line Markers in library files
- Fixed: Improve Yaml/properties completion

### Other

- Added: User usage statistics gathering

## 243.1.2727 - 2024-11-23

### Spring Core/Boot

- Added: @EnableConfigurationProperties annotation marks @ConfigurationProperties as beans
- Added: Inspection and QuickFix checks if @ConfigurationProperties class is correctly configured
- Fixed: Yaml property value completion doesn't work (#9)

### OpenAPI

- Fixed: backward navigation to `$ref` usage
- Added: endpoint linemarker suggests openapi generation when there is no place to navigate to (#12)
- Added: intention to generate description from controller endpoint

### Other

- Fixed: fix EDT while editing json schema files

## 243.1.2662 - 2024-11-15

### Spring Core/Boot

- Added: completion and inspection in `additional-metadata.json`
- Added: native SQL support integration with Database Navigator
- Added: schema to additional configuration metadata
- Fixed: inspection kebab-case in properties for Map
- Fixed: navigate in properties for Map
- Fixed: inserting one parameter instead of several
- Fixed: wrong bean method navigation
- Fixed: Don't warn about kebab case for property values
- Fixed: profile change (#6)
- Fixed: Bug: bean line marker native fix
- Fixed: Inspection SpringBeanIncorrectAutowiringInspection false positive ConfigurationProperties
- Fixed: Property navigation from application.properties from tests
- Fixed: module AlreadyDisposedExceptional

### Other

- Prepare code configurations for open source
- Fixed: incorrect CachedValue use for ConfigurationPropertyDataRetriever
- Support 243 idea plugin sdk
- Enable K2 support mode

### Spring Web/MVC

- Show/hide the Explyt Endpoints if the project has/does a web dependencies
- Fixed: in Explyt Endpoints show in path one slash (not empty path)
- Fixed: in Explyt Endpoints not show empty list
- Fixed: gutter calculation for coRouter route

### OpenAPI

- Fixed: inspection for OpenAPI version 

## 242.1.2334 - 2024-10-29

### Spring Web/MVC

- Added ToolWindow with endpoints
- Added inspection for @LoadBalanced annotation in the FeignClient interface
- Fixed: Inspection in @ConfigurationProperties with @ConstructorBinding is in use, by binding to the constructor
  parameters
- Fixed: PatternSyntaxException while parsing controller
- Added endpoint loaders for FeignClient, RestClient, and WebClients

### OpenAPI

- Added inspection for a supported version of OpenApi
- Added completion in OpenAPI specification
- Automatically apply OpenAPI JSON schema (Specification 3.0.0/3.1.0)
- Supported dark theme for SwaggerUI panel
- Added navigation from OpenAPI http request to preview (SwaggerUI panel)
- Added OpenAPI files preview (SwaggerUI panel)
- Added OpenAPI `$ref` navigation and completion for .yaml/.json formats
- Added endpoint loaders for OpenAPI
- Added special icons for OpenAPI files
- Fixed: Dismiss Ultimate promo bar for OpenAPI files

### Spring Core/Boot

- Added the Spring Native Beans panel for Spring Boot applications
- Added support Spring Boot versions before 2.4.0 via explyt.spring.native.old flag
- Added icon for a configuration file for Spring Boot application
- Added reference to package from annotation parameters with names `basePackages`, `scanBasePackages`. AntPattern supported
- Changed behaviour of `Can't find package` inspection. Now it shows inspection till first unknown segment. AntPattern supported
- Added linemarker from property to library hint
- Fixed: property completion for class-reference provider from libraries
- Fixed: prefixFromUsage cache bug
- Fixed: AlreadyDisposedException
- Added: Generator for @PostConstruct methods
- Fixed: autowired fields in abstract class
- Fixed: esprito.spring.root.runConfiguration
- Fixed: Detect @Component bean for abstract class

### Spring AOP

- Added Spring AOP processing using Spring Boot Native

### Other

- Upgrade to intellij 242 branch
- Endpoints tool icon light

## 241.1.1834 - 2024-08-06

### Spring Core/Boot

- Fixed: Autocomplete in application.properties produces an error: NoSuchElementException: List is empty.
- Fixed: @ConfigurationProperties with lombok @Setter and Lombok plugin installed produces an error
- Added Inspection: `Should be kebab-case` for property key
- Fixed: Yaml property autocomplete inserts into previous line
- Fixed: kebab-case inspection description
- Fixed: Yaml PropertyLineMarker only for leaf elements
- Added beans search by name in `Search Everywhere`
- Performance: ConfigurationProperty prefix calculation was too slow
- Performance: Improves after performance tests on big projects
- Fixed: ConditionOnProperty is enabled even if it was disabled in properties
- Fixed @ConfigurationProperties - navigation/line markers

### Spring Data

- Fixed: support CoroutineCrudRepository
- Fixed: Repository injected through @Autowired and package enabled via @EnableJpaRespoitory
- Inspection: @EntityScan package support
- Fixed: Spring Data method name inspection - default interface methods were not analyzed

### Spring Web/MVC

- Updated: navigation between endpoint with template parameters and  `WebTestClient`, `MockMvcBuilders` methods
- Add navigation considering `WebTestClient.method` to endpoint. Add elements to endpoint's linemarker navigating to uri considering `WebTestClient.method`, `MockMvcBuilders.request`, `MockMvcBuilder.multipart`
- Add a gutter to the methods `RouterFunctions`: `coRouter`, `route`
- Fixed: navigation from endpoint's gutter
- Add `WebTestController` `expectBody[List]` methods autocompletion
- Add `RouterFunctions` `coRouter` methods autocompletion
- Add `RouterFunctions` `route` methods autocompletion
- Performance: endpoint usage search speed-up

### Other

- compatibility with internal 242 api 
- gradle kotlin module test problem
- Add error tracking via Sentry setup

## 241.1.1581 - 2024-06-10

### Spring Core/Boot

- Fixed: Inspection: Duplicate properties keys in different relaxed binding forms do not show error
- Find Usages: for property keys when used in different relaxed binding forms
- Fixed: Navigate to autowired candidates and bean declarations for wildcard type
- Fixed: Navigate to autowired candidates and bean declarations in Kotlin: arrays, collections and maps
- Make Spring `@Scheduled` description human-readable
- Inspection: property placeholder not in a kebab-case
- Fixed: Inspection "Cannot resolve key property" for non-kebab-case keys
- Fixed: Inspection "Autowire failed" for `ApplicationContext`
- Inspection: Kotlin `internal` modifier mangling
- The inspection text has been adjusted for `@ConfigurationProperties`
- Intention: create property description in `additional-metadata.json`
- Added reference from `DynamicPropertyRegistry.add` method to property
- Fixed inspection `resource name must begin with a slash`. Added valid prefixes
- Fixed `@Value` folding for Kotlin

### Spring Web/MVC

- Inspection: `WebController` `bodyToMono/Flux`, `awaitBody` type doesn't match endpoint
- Add `WebController` `bodyToMono/Flux`, `awaitBody` methods autocompletion
- Inspection: `WebController/WebTestController` uri parameters count
- Add Navigation from `WebClient/WebTestClient` to Controller endpoints

### Other

- Migrate Esprito to Explyt
- Update plugin description
- Fixed: Stale UAST cache

## 241.1.1303 - 2024-04-24

### Spring Core/Boot

- Added a gutter to the constructors of other classes that are used in the class with @ConfigurationProperties
- Added autocompletion in properties files for all classes fields
- Inspection `unresolved property key` supports Relaxed Binding
- Fixed inspection for `@Retention` on Spring annotations. Supported meta-annotations
- Fixed stackoverflow error while property calculation on some cases
- Support `@Scheduled` annotation

## 241.1.1263 - 2024-04-17

### Spring Core/Boot

- Fixed inspection for constructor parameters with default value: Class constructor properties annotated
  with `@ConfigurationProperties` must be nullable
- Property reference supports Relaxed Binding
- Auto-detection for Kotlin SpringBoot run configurations

### Other

- Added license check scheduler
- Fix: EDT exception in SpringToolRunConfigurationConfigurable

## 241.1.1199 - 2024-04-11

### Spring Core/Boot

- Inspection `unresolved property key` supports Relaxed Binding
- Fix: Kotlin constructor with default values counts as autowired
- Inspection: Warning: Spring @Value annotation string should start with "#{" "${" or resource prefixes

### Other

- License URL fix

## 241.1.1167 - 2024-04-09

### Spring Core/Boot

- LineMarker: Navigation from Kotlin constructor parameters to property declaration

### Spring Data

- Bean method doesn't navigate to autowired collection

### Other

- Upgrade to intellij 241 branch

## 2024.233.1151 - 2024-04-08

### Spring Data

- Spring Data methods name inspection (Repository: findAll property is unknown)
- Parse run configuration profile arguments
- LineMarker: Navigate to Bean Declaration for class/type injected through the @Bean annotation

### Spring Core/Boot

- Added inspection: Class constructor properties annotated with `@ConfigurationProperties` must be nullable
- Added `'ConditionalOn' bean filtering` setting with `true` as default value. On activation bean filtering works
  without auto-configurations
- Detect `context.register` as context root
- Support @SpringBootTest annotation
- LineMarker: Inactive bean
- Fix: reference publishEvent to inherited annotation listener
- Fix: inspection reference placeholder in yaml

### Spring Web/MVC

- Inspection for duplicated `@RequestMapping` endpoints

### Other

- Invalid name in Tools settings
- Added validate license panel

## 2024.233.1020 - 2024-03-22

### Spring Core/Boot

- Support `@ComponentScan` scope to find spring beans.
- Support `@Import` to find configurations.
- Inspection for methods annotated with `@Async`, `@Transactional`, `@Cacheable`, `@CachePut`, `@CacheEvict` work to prevent calls within the same class.
- Bean scope options in autocomplete, including custom scopes.
- Inspections for missing or problematic resource files in .properties and .yaml.
- Inspection for interfaces annotated with `@Cacheable`, `@CacheConfig`, `@CachePut`, `@CacheEvict`, `@Caching`: prohibit cache annotations on interfaces.
- Line marker navigates from `getBean` method to bean declaration.
- Fix: Remove or comment Bean class action does not recover (return back) bean gutter icon

### Spring Web/MVC

- Add Navigation from strings "redirect:" to controller endpoints.
- Enhanced MockMvc with better handling of multipart requests and parameter checks.
- Linemarkers for quick navigation to URLs defined in controller endpoints.
- Integrated OpenAPI spec navigation for both .yaml and .json formats.

### Spring Security

- Included reference checks for Spring beans within Spring Security annotations.

### Spring Data

- Detect JpaRepositories as beans.

### Other

- Switch to versioning 2024.{platformVersion}.{buildNumber}.
- Inspection for getResource method to ensure classpath resource path is correct.
- Inspections and tests to improve overall functionality.
- Automated Changelog introduced.
- Extended tests coverage for cases: bean inheritance, bean name navigation, bean as parameter.
- Added validate license panel in Settings/Tools/Esprito Spring Tools.
- Fix: Adjust inspection paths and keys.
- Fix: Set name Esprito Spring Tools in Settings/Tools.
