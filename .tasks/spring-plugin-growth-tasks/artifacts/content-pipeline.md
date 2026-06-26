# English Content Pipeline

**Task:** 06 · **Type:** Documentation · **Date:** 2026-06-26  
**Goal:** turn the strongest RU-first Explyt Spring technical material into English content that can be discovered by Spring, Kotlin, IntelliJ IDEA Community, and AI-agent audiences.

---

## 1. Operating rules

1. **Adapt, do not literal-translate.** Lead with the developer problem, show a working code/debug/agent workflow early, then explain the implementation. RU Habr-style long narrative should be shortened for EN audiences.
2. **Canonical first, cross-post second.** Publish on Medium (`@explytspring`) as canonical. Cross-post to dev.to and Foojay with `rel=canonical`/canonical URL pointing back to Medium.
3. **Measure every CTA.** Use UTM parameters on install/docs/GitHub links. Change `utm_source` per venue (`medium`, `devto`, `foojay`, `reddit`, `hackernews`, `telegram`, `thisweekinspring`).
4. **No unverifiable superlatives.** Use the Task 02 positioning: “runtime-accurate Spring tooling for IntelliJ IDEA Community — free.” Avoid “Ultimate replacement,” “only,” and “best.”
5. **Be transparent in communities.** Reddit/HN posts should be by a maintainer, with a short disclosure, and the author must stay in comments. Do not repost the same link across many subreddits on the same day.
6. **One hero asset per article.** First screen should include a GIF/screenshot or code block: debugger tree, MCP tool call, or Native Context Mode loading beans.

### Cadence rule

Publish **one English technical article per release cycle**, ideally during the release-candidate or release week so the article can link to concrete “What’s New” items. If a release cycle is long, publish at least one evergreen article every 6–8 weeks and refresh it in the next release notes.

---

## 2. Status tracker

| # | Article | Primary venue | Cross-posts | Status | Next action |
|---|---|---|---|---|---|
| 1 | Debug Spring, Not Just Java: Spring Context in the IntelliJ Debugger | Medium | dev.to, Foojay | **Draft ready** in §6 | Editorial pass + capture debugger GIF |
| 2 | Give AI Agents Spring Semantics: MCP Tools for Beans, Endpoints, and JPA | Medium | dev.to, Foojay | Brief ready | Draft next; strongest HN candidate |
| 3 | Runtime-accurate Spring Tooling: Javaagent + Bytecode Patching for Native Context Mode | Medium refresh/update | dev.to, Foojay | Brief ready | Refresh existing EN Medium article and add current details |

---

## 3. Shared UTM templates

Replace `{source}` with `medium`, `devto`, `foojay`, `reddit`, `hackernews`, `telegram`, or `thisweekinspring`; replace `{content}` with the article slug below.

- Marketplace install: `https://plugins.jetbrains.com/plugin/28675-spring-explyt?utm_campaign=english_content&utm_source={source}&utm_medium=article&utm_content={content}`
- GitHub repo/star: `https://github.com/explyt/spring-plugin?utm_campaign=english_content&utm_source={source}&utm_medium=article&utm_content={content}`
- Marketplace reviews: `https://plugins.jetbrains.com/plugin/28675-spring-explyt/reviews?utm_campaign=english_content&utm_source={source}&utm_medium=article&utm_content={content}`
- Docs: `https://explyt.ai/docs/category/explyt-spring?utm_campaign=english_content&utm_source={source}&utm_medium=article&utm_content={content}`
- Telegram EN: `https://t.me/explytspring_en`

Per article, use `utm_medium=article` inside the article body, `utm_medium=social` for Reddit/HN/Telegram posts, and `utm_medium=newsletter` for This Week in Spring submissions.

---

## 4. Article brief 1 — Spring Debugger

### Working title

**Debug Spring, Not Just Java: Bringing the Application Context into the IntelliJ Debugger**

### Source material

- RU Habr: <https://habr.com/ru/companies/explyt/articles/933158/>
- Product copy: `README.md` “Spring Debugger”; `PLUGIN-DESCRIPTION.md` “Spring Debugger”
- Implementation references:
  - `modules/spring-core/src/main/kotlin/com/explyt/spring/core/debug/SpringDebuggerRunConfigurationExtension.kt`
  - `modules/spring-core/src/main/kotlin/com/explyt/spring/core/debug/SpringDebuggerContextRenderer.kt`
  - `modules/spring-core/src/main/kotlin/com/explyt/spring/core/hint/PropertyDebugValueCodeVisionProvider.kt`
  - `modules/spring-core/src/main/kotlin/com/explyt/spring/core/providers/DebugBeanDefinitionLineMarkerProvider.kt`
  - `modules/spring-data/src/main/kotlin/com/explyt/spring/data/RepositoryDebugMethodLineMarkerRunProvider.kt`
  - `explyt-spring-boot-bean-reader/explyt-context-holder/src/main/java/explyt/Explyt.java`

### Angle for EN audience

Spring bugs often hide in runtime state: the actual bean graph, proxies, active transaction settings, and `PropertySource` precedence. Java debuggers show objects well, but not always the Spring context that produced them. Explyt Spring adds Spring-specific runtime context to the IntelliJ debugger in Community Edition.

### Outline

1. Hook: “The debugger stopped, but the Spring question is still unanswered.”
2. Minimal Kotlin/Spring example: overridden property + transactional service + repository method.
3. What the standard debugger shows vs. what Spring developers need.
4. Explyt Spring Debugger features:
   - Spring Context / Contexts node in debugger tree.
   - BeanFactory and Environment access.
   - Active Transaction node with isolation/read-only information.
   - Runtime property Code Vision in `.properties`/`.yml`.
   - Evaluate Expression helpers (`Explyt.context`, `Explyt.getBean(...)`, `Explyt.getProperty(...)`, `Explyt.getBeanDefinition(...)`).
   - Repository/bean gutter actions that pre-fill Evaluate Expression.
5. How it works at a high level: a lightweight javaagent exposes Spring runtime data to IDE debugger renderers.
6. Limits and honesty: needs a stopped debug session; not a full IDEA Ultimate replacement; strongest value is focused Spring debugging for IDEA Community.
7. CTA: install Spring Explyt, run a small Boot app, star/rate if it helps.

### Hero asset / sample

- GIF: breakpoint in a service method → expand `Explyt Spring Context Data` → inspect `Active Transaction` → click runtime property Code Vision.
- Sample code can be the Kotlin snippets in §6; optional companion gist name: `spring-debugger-runtime-context-demo`.

### Primary and cross-post plan

- Canonical: Medium `@explytspring`.
- Cross-post: dev.to and Foojay, with canonical URL set to the Medium URL after publish.
- Suggested slug/content id: `spring_debugger`.

### CTA links for article body

- Install: `https://plugins.jetbrains.com/plugin/28675-spring-explyt?utm_campaign=english_content&utm_source=medium&utm_medium=article&utm_content=spring_debugger`
- GitHub: `https://github.com/explyt/spring-plugin?utm_campaign=english_content&utm_source=medium&utm_medium=article&utm_content=spring_debugger`
- Reviews: `https://plugins.jetbrains.com/plugin/28675-spring-explyt/reviews?utm_campaign=english_content&utm_source=medium&utm_medium=article&utm_content=spring_debugger`

### Distribution checklist

- [ ] Submit to This Week in Spring using `utm_source=thisweekinspring&utm_medium=newsletter&utm_content=spring_debugger`.
- [ ] Post to r/java and r/SpringBoot with maintainer disclosure and a debugging-specific summary.
- [ ] Post to Telegram EN channel.
- [ ] Consider r/Kotlin only if the final article keeps the Kotlin sample as the primary demo.
- [ ] Do **not** use HN for this one unless the debugger GIF is especially strong; save the one-shot HN attempt for MCP.

---

## 5. Article brief 2 — Spring-aware MCP tools

### Working title

**Give AI Agents Spring Semantics: MCP Tools for Beans, Endpoints, and JPA in IntelliJ IDEA**

### Source material

- RU Habr: <https://habr.com/ru/articles/986226/>
- Local draft/source: `Spring MCP.md`
- Implementation references:
  - `modules/spring-ai/src/main/kotlin/com/explyt/spring/ai/mcp/SpringMcpProvider.kt`
  - `modules/spring-ai/src/main/resources/META-INF/mcp-server-plugin.xml`
  - `modules/spring-ai/src/test/kotlin/com/explyt/spring/ai/mcp/SpringBootApplicationMcpToolsetTest.kt`
  - `modules/spring-ai/src/test/kotlin/com/explyt/spring/ai/mcp/SpringBootApplicationMcpToolsetTraceCallChainTest.kt`

### Angle for EN audience

Generic coding agents can grep a Spring project, but they do not automatically know which class is a bean, which controller method owns an endpoint, or how a DTO maps into an API contract. Explyt Spring adds Spring-aware MCP tools on top of JetBrains’ bundled MCP Server so agents can ask the IDE for Spring semantics directly.

### Accurate claims to preserve

- Requires IntelliJ IDEA 2025.2+ with bundled JetBrains MCP Server.
- Exposes exactly **7** Spring-aware MCP tools:
  1. `explyt_get_spring_boot_applications`
  2. `explyt_get_project_beans_by_spring_boot_application`
  3. `explyt_find_spring_endpoint`
  4. `explyt_get_spring_http_endpoints`
  5. `explyt_get_spring_endpoint_contract`
  6. `explyt_trace_spring_call_chain`
  7. `explyt_get_spring_data_entities`
- Tool outputs cover Spring Boot apps, beans by type, endpoint search/listing, endpoint contracts with DTO schema, Controller → Service → Repository call chains, tests, and JPA entity/table/column/relationship/index metadata.
- Avoid “only Spring MCP tools” unless a fresh competitor scan proves exclusivity. Use “ships Spring-aware MCP tools.”

### Outline

1. Hook: agents waste context budget rediscovering Spring structure by reading files.
2. Short MCP + JetBrains MCP Server primer.
3. How a plugin contributes a toolset (`McpToolset`, `@McpTool`, `@McpDescription`).
4. Before/after prompt: “What endpoints does this app expose?” with generic tools vs. Spring-aware tools.
5. Tour of the 7 tools grouped by application, beans, endpoints/contracts, data, and call chain.
6. Practical prompts developers can use:
   - “Find the endpoint that handles `POST /orders` and show its DTO schema.”
   - “Trace this controller to repositories and tests.”
   - “List JPA entities touching table `orders`.”
7. Limits: Spring Boot scope; IDEA 2025.2+; quality still depends on project indexing and loaded model.
8. CTA: install Spring Explyt + connect MCP Server to an agentic client.

### Hero asset / sample

- Side-by-side screenshot or GIF: agent with generic tools makes many searches; same prompt with Explyt MCP calls `explyt_get_spring_endpoint_contract` and returns the endpoint contract.
- Optional sample: use a small Spring Boot REST/JPA project with `OrderController`, `OrderService`, `OrderRepository`, and DTOs.

### Primary and cross-post plan

- Canonical: Medium `@explytspring`.
- Cross-post: dev.to and Foojay with canonical URL.
- Suggested slug/content id: `spring_mcp_tools`.

### CTA links for article body

- Install: `https://plugins.jetbrains.com/plugin/28675-spring-explyt?utm_campaign=english_content&utm_source=medium&utm_medium=article&utm_content=spring_mcp_tools`
- GitHub: `https://github.com/explyt/spring-plugin?utm_campaign=english_content&utm_source=medium&utm_medium=article&utm_content=spring_mcp_tools`
- Docs: `https://explyt.ai/docs/category/explyt-spring?utm_campaign=english_content&utm_source=medium&utm_medium=article&utm_content=spring_mcp_tools`

### Distribution checklist

- [ ] Submit to This Week in Spring with `utm_content=spring_mcp_tools`.
- [ ] Post to r/java and r/SpringBoot; consider r/Jetbrains if rules allow plugin posts.
- [ ] **Use Hacker News “Show HN” for this article** if the demo asset is strong: “Show HN: Spring-aware MCP tools for IntelliJ IDEA agents”.
- [ ] Post to Telegram EN.
- [ ] Share short clips on X/LinkedIn from maintainer account if available.

---

## 6. Article brief 3 — Native Context Mode / bytecode patching

### Working title

**Runtime-accurate Spring Tooling: How a Javaagent Helps IntelliJ IDEA Community See Real Beans**

### Source material

- Existing EN Medium article to refresh: <https://medium.com/@explytspring/explyt-spring-plugin-patching-spring-bytecode-to-enhance-application-context-recognition-0817fb52b056>
- Complementary EN Medium article: <https://medium.com/@explytspring/stop-playing-catch-up-with-spring-introducing-the-explyt-spring-plugin-for-idea-community-0be380b36a75>
- Implementation references:
  - `modules/spring-core/src/main/kotlin/com/explyt/spring/core/externalsystem/SpringBeanNativeResolver.kt`
  - `modules/spring-core/src/main/kotlin/com/explyt/spring/core/runconfiguration/SpringToolRunConfigurationsSettingsState.kt`
  - `explyt-spring-boot-bean-reader/java-agent/src/main/java/io/github/grisha9/PatchAgentPremain.java`
  - `explyt-spring-boot-bean-reader/java-agent/src/main/java/com/explyt/spring/boot/bean/reader/AbstractApplicationContextDecorator.java`
  - `explyt-spring-boot-bean-reader/java-agent/src/main/java/com/explyt/spring/boot/bean/reader/AspectJAopUtilsDecorator.java`
  - `explyt-spring-boot-bean-reader/java-agent/src/main/java/com/explyt/spring/boot/bean/reader/InternalHolderContext.java`

### Angle for EN audience

Static Spring analysis is useful, but conditions, profiles, factory beans, library beans, generated proxies, AOP, and multi-context applications are runtime problems. Native Context Mode uses a lightweight javaagent run to collect real Spring metadata, then brings that data back into IntelliJ IDEA Community for navigation and inspections.

### Accurate claims to preserve

- Native Context Mode is default-on in settings (`isJavaAgentMode=true`).
- Explyt patches Java/Kotlin run configurations with a javaagent for the lightweight bean-loading run.
- The run is intentionally lightweight: it starts enough Spring initialization to capture metadata and then exits through an Explyt marker rather than requiring a full application lifecycle.
- Captured data includes bean names/classes/scopes/primary flags, factory method/bean data, Spring Data repository type, multiple contexts, and AOP advisor-to-method mappings.
- Use the real path `io/github/grisha9/PatchAgentPremain.java` for the premain class.

### Outline

1. Hook: static analysis says “no bean,” but the app runs fine — why?
2. Why Spring is hard for static tools: profiles, conditions, factories, library auto-configuration, proxies, AOP, multiple contexts.
3. Native Context Mode from the user perspective: Link run configuration → Load Beans → IDE model updates.
4. Under the hood: javaagent, declarative bytecode decorators, Spring startup interception.
5. Metadata collected and how it improves navigation/inspections.
6. AOP and multi-context examples.
7. Why this is not a full app run and what the limits are.
8. CTA: install, load beans, compare static vs runtime model.

### Hero asset / sample

- GIF: warning/unknown bean before Load Beans → click Load Beans → conditional/factory/library bean becomes navigable.
- Sample repo/gist: one conditional bean, one factory bean, one AOP aspect, one Spring Data repository.

### Primary and cross-post plan

- Canonical: update/refresh Medium `@explytspring` article if possible; otherwise publish a new “2026 update” and link from the old article.
- Cross-post: dev.to and Foojay with canonical URL.
- Suggested slug/content id: `native_context_mode`.

### CTA links for article body

- Install: `https://plugins.jetbrains.com/plugin/28675-spring-explyt?utm_campaign=english_content&utm_source=medium&utm_medium=article&utm_content=native_context_mode`
- GitHub: `https://github.com/explyt/spring-plugin?utm_campaign=english_content&utm_source=medium&utm_medium=article&utm_content=native_context_mode`
- Docs: `https://explyt.ai/docs/category/explyt-spring?utm_campaign=english_content&utm_source=medium&utm_medium=article&utm_content=native_context_mode`

### Distribution checklist

- [ ] Submit to This Week in Spring with `utm_content=native_context_mode`.
- [ ] Post to r/java and r/SpringBoot; consider r/Kotlin only if Kotlin-specific conditional/factory examples are included.
- [ ] Post to Telegram EN.
- [ ] Link from README “Learn More” after publish.
- [ ] Mention in next release notes if native-context improvements ship in the same cycle.

---

## 7. First full draft — Spring Debugger article

> **Publishing note:** This draft is intentionally written as an adaptation, not a translation. It uses a compact Kotlin/Spring example for an EN audience. Before publishing, add the debugger GIF described in §4 and replace the plain Medium CTA links with the final per-venue UTM source when cross-posting.

# Debug Spring, Not Just Java: Bringing the Application Context into the IntelliJ Debugger

You hit a breakpoint in a Spring Boot application.

The local variables are there. The call stack is there. The object you are inspecting is there.

But the question you actually need to answer is not always a Java question:

- Which bean implementation did Spring inject here?
- Is this method currently inside a real Spring transaction?
- Which `PropertySource` won for this configuration value?
- What does the proxied repository/service bean look like at runtime?
- Can I call this bean method right now without writing temporary debug code?

That is the gap Explyt Spring Debugger tries to close for IntelliJ IDEA Community users: keep the normal IntelliJ debugger, but add Spring runtime context next to it.

Explyt Spring is a free, source-available Spring plugin for IntelliJ IDEA Community Edition. The debugger is one part of it: it adds Spring Context data, active transaction information, runtime property values, and Spring-aware Evaluate Expression helpers to your debug session.

## A small example

Imagine a Kotlin Spring service like this:

```kotlin
@Service
class PricingService(
    private val orderRepository: OrderRepository,
    @Value("${pricing.discount-percent:0}")
    private val discountPercent: Int,
) {
    @Transactional(readOnly = true)
    fun calculateTotal(orderId: Long): Money {
        val order = orderRepository.getReferenceById(orderId)
        return order.total.minusPercent(discountPercent)
    }
}
```

And a property file:

```properties
pricing.discount-percent=5
```

In production-like runs, that value may be overridden by a profile, environment variable, command-line argument, Kubernetes secret, test configuration, or another `PropertySource`.

When something goes wrong, the code alone is not enough. You want to stop inside `calculateTotal` and ask Spring what is really happening.

## What a Java debugger already does well

A normal debugger is excellent at Java/Kotlin runtime state:

- local variables;
- fields;
- stack frames;
- exceptions;
- threads;
- Evaluate Expression.

That is necessary, but Spring applications add another runtime layer. A lot of important information is owned by the application context, not by the current object.

For example, `PricingService` may be a proxied bean. The repository may be a Spring Data proxy. The transaction may be controlled by Spring AOP. The property value may come from a different source than the file you are currently reading.

So the useful debugging question becomes: “What does Spring know right now?”

## What Explyt adds to the debugger

When you start a Spring Boot app in Explyt debug mode and stop at a breakpoint, Explyt adds Spring-specific runtime data to the debugger UI.

### 1. Spring Context data in the debugger tree

The debugger gets an `Explyt Spring Context Data` node. From there you can inspect the active Spring context, multiple contexts, the bean factory, and the environment.

That is useful when the code path depends on what Spring actually registered:

- conditional beans;
- profile-specific beans;
- factory beans;
- library/auto-configuration beans;
- proxies;
- multiple application contexts.

Instead of mentally reconstructing the Spring model from annotations and configuration files, you can inspect the runtime model while the app is stopped.

### 2. Active Transaction information

If the current execution is inside a Spring transaction, Explyt shows an `Explyt: Active Transaction` node.

This is the kind of information that is easy to forget until it breaks something:

- Is a transaction active at this breakpoint?
- Is it read-only?
- What isolation level is Spring using?
- Which transaction metadata is available from Spring’s transaction infrastructure?

For debugging persistence bugs, lazy loading surprises, and accidental writes inside read-only flows, this is much faster than adding temporary logs.

### 3. Runtime property values in `.properties` and `.yml`

Configuration bugs are often precedence bugs.

You open `application.properties` and see:

```properties
pricing.discount-percent=5
```

But the runtime value is `15`, because another `PropertySource` won.

During a stopped Explyt debug session, Explyt can show runtime property values as Code Vision in `.properties` and `.yml` files. The goal is not just “what is the value?” but also “where did this value come from?”

That turns a common Spring debugging loop:

1. add logging;
2. restart;
3. check output;
4. search for overrides;
5. repeat;

into a direct IDE workflow while the debugger is already stopped.

### 4. Spring-aware Evaluate Expression helpers

Evaluate Expression is one of the most powerful debugger features, but in Spring apps you usually need access to the application context first.

Explyt exposes helper entry points such as:

```java
Explyt.context
Explyt.getBean(PricingService.class)
Explyt.getProperty("pricing.discount-percent")
Explyt.getBeanDefinition("pricingService")
```

That means you can ask Spring for a bean, property, or bean definition without writing temporary code just to reach the context.

For Spring Data repositories, Explyt can also provide debug run markers that prepare Evaluate Expression calls for repository methods. In practice, this makes common “what would this repository return here?” checks quicker during a paused debug session.

## How it works at a high level

The implementation has two sides.

On the application side, Explyt attaches a lightweight javaagent to the debug run. The agent exposes selected Spring runtime objects and helper methods in a way the IDE can query while the process is paused.

On the IDE side, Explyt contributes debugger renderers, extra debug nodes, Code Vision providers, and gutter actions. These pieces turn the runtime Spring data into something visible and clickable in IntelliJ IDEA.

You do not have to replace the IntelliJ debugger. The idea is to enrich it with Spring-specific context.

## What this is good for

Explyt Spring Debugger is especially useful when the bug depends on runtime Spring behavior:

- a bean exists only under a profile or condition;
- an injected dependency is not the implementation you expected;
- a transaction is missing, read-only, or has surprising settings;
- a property value differs between file and runtime;
- a repository/service method is easier to evaluate from the context than from the current object graph;
- you are using IntelliJ IDEA Community Edition and want focused Spring debugging support.

It is not meant to be a replacement for understanding Spring. It is meant to remove the mechanical work between “I have a breakpoint” and “I can see the Spring state that matters.”

## Honest limits

A few practical notes:

- You need a running debug session stopped at a breakpoint.
- Runtime property and transaction data are only available when the application state exposes them.
- Some functionality depends on Spring Boot/Spring APIs and the project being indexed by the IDE.
- Explyt Spring is not a full IntelliJ IDEA Ultimate replacement. It is a focused Spring toolkit for IntelliJ IDEA Community, with runtime-aware features where that matters.

## Try it

1. Install **Spring Explyt** from JetBrains Marketplace: <https://plugins.jetbrains.com/plugin/28675-spring-explyt?utm_campaign=english_content&utm_source=medium&utm_medium=article&utm_content=spring_debugger>
2. Open a Spring Boot project in IntelliJ IDEA Community Edition.
3. Start a Spring debug session with Explyt enabled.
4. Stop inside a service method.
5. Expand the Spring context data, inspect the active transaction, and try `Explyt.getBean(...)` or `Explyt.getProperty(...)` in Evaluate Expression.

If it saves you debugging time, a GitHub star or Marketplace rating helps other Spring developers find it:

- GitHub: <https://github.com/explyt/spring-plugin?utm_campaign=english_content&utm_source=medium&utm_medium=article&utm_content=spring_debugger>
- Marketplace reviews: <https://plugins.jetbrains.com/plugin/28675-spring-explyt/reviews?utm_campaign=english_content&utm_source=medium&utm_medium=article&utm_content=spring_debugger>

---

## 8. Post-publication checklist template

Use this checklist for each article after Medium publication:

- [ ] Medium URL recorded here.
- [ ] dev.to cross-post published with canonical URL.
- [ ] Foojay cross-post submitted/published with canonical URL.
- [ ] README “Learn More” updated if the article becomes evergreen product documentation.
- [ ] Telegram EN post published.
- [ ] This Week in Spring submission sent.
- [ ] Reddit posts made according to subreddit rules; comments monitored for 48 hours.
- [ ] HN attempted only for the MCP article, unless strategy changes.
- [ ] UTM clicks checked in the next Task 01 metrics routine.
