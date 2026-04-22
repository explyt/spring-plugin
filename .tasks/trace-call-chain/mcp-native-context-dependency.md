# MCP Tools: Dependency on Native Context (Linking)

## Summary

**All 4 MCP tools work WITHOUT linking (native context).** They exclusively use static analysis (PSI, annotation indices, UAST). None of them route through `SpringSearchServiceFacade`, which is the gateway that decides between static vs. native bean resolution.

---

## Tool-by-Tool Analysis

### 1. `explyt_find_spring_endpoint`

**Call chain:**
```
SpringMcpProvider.findEndpoint()
  → SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints()
    → iterates project.modules
      → SpringWebEndpointsLoader.EP_NAME extensions (e.g., SpringWebControllerLoader)
        → SpringWebControllerLoader.doSearchEndpoints(module)
          → MetaAnnotationUtil.getAnnotationTypesWithChildren(module, CONTROLLER, false)
          → searchAnnotatedClasses(annotationClass, module)  // index-based
          → MetaAnnotationsHolder.of(module, REQUEST_MAPPING)  // annotation holder
          → getEndpoints(psiClass, requestMappingMah)  // walks methods via PSI
```

**Verdict: STATIC ONLY.** Uses `MetaAnnotationUtil` (annotation index), `AnnotatedElementsSearch` (PSI index), and `MetaAnnotationsHolder` (annotation hierarchy). No native context data.

**What works without linking:** Everything. All endpoints from `@Controller`/`@RestController` classes with `@RequestMapping`, `@GetMapping`, etc. are discovered via annotation indices.

**What would be missing:** Nothing. This tool doesn't consult bean context at all — it finds controllers by annotation, not by bean registration.

---

### 2. `explyt_trace_spring_call_chain`

**Call chain:**
```
SpringMcpProvider.traceCallChain()
  → Resolves file → PsiManager → findElementAt → getUastParentOfType<UMethod>()
  → buildChain(psiMethod, depth, visited, project)
    → findCalledMethods(psiMethod)  // UAST: UCallExpression.resolve()
    → detectSpringLayer(psiClass)   // isMetaAnnotatedBy(@Controller, @Service, etc.)
  → findTestReferences(methods, module, project)
    → MethodReferencesSearch.search(method, testScope)
```

**Verdict: STATIC ONLY.** Uses UAST traversal for method body analysis, `isMetaAnnotatedBy` for annotation checks, and `MethodReferencesSearch` for test references. All PSI/index-based.

**What works without linking:** Full call chain traversal, Spring layer detection, test reference discovery.

**What would be missing:** Nothing. This tool traces code structure, not runtime bean wiring.

---

### 3. `explyt_get_project_beans_by_spring_boot_application`

**Call chain:**
```
SpringMcpProvider.applicationBeans()
  → McpBeanSearchService.getInstance(project).getProjectBeansMcp(module)
    → CachedValuesManager (cached)
      → McpBeanSearchService.getProjectBeans(module)
        → SpringSearchService.getInstance(project).getProjectBeans(module)   // ← STATIC method
          → SpringSearchUtils.getComponentClassAnnotations(module)
          → PackageScanService.getInstance(project).getAllPackages()
          → searchBeanPsiClassesByAnnotations(module, annotations, scope)
          → getExtraComponents(module, modulePackagesHolder)
          → getImportedBeans(modulePackagesHolder, module)
          → searchComponentPsiClassesByBeanMethods(psiBeans)
        → MessageMappingEndpointLoader.searchMessageMappingClasses(module, scope)
      → McpBeanSearchService.getBeanType(psiClass, mappingClasses)
        → isMetaAnnotatedBy(@Controller, @Repository, @Configuration, etc.)
```

**Verdict: STATIC ONLY.**

**Key insight:** `McpBeanSearchService` calls `SpringSearchService.getProjectBeans(module)` directly (line 49 of McpBeanSearchService.kt) — **NOT** `SpringSearchServiceFacade`. The facade (`SpringSearchServiceFacade`) is where the `isExternalProjectExist()` check routes to `NativeSearchService` vs. `SpringSearchService`. Since the MCP tool bypasses the facade, it always gets static beans.

**What works without linking:** All beans discoverable through:
- `@Component` / `@Service` / `@Controller` / `@Repository` / `@Configuration` annotations
- `@Bean` methods in `@Configuration` classes
- `@Import` annotations
- Package scanning via `@ComponentScan` / `@SpringBootApplication`
- Extra bean discoverers (EP)

**What would be missing:** 
- Beans from `@Conditional*` evaluation would NOT be filtered accurately (static analysis guesses, native context knows for sure)
- Beans registered programmatically (e.g., via `BeanDefinitionRegistryPostProcessor`) would be missing
- Beans from third-party auto-configurations that rely on complex conditional logic might be incorrectly included/excluded

---

### 4. `explyt_get_spring_boot_applications`

**Call chain:**
```
SpringMcpProvider.getAllSpringBootApplications()
  → PackageScanService.getInstance(project).getSpringBootAppAnnotations()
    → LibraryClassCache.searchForLibraryClass(project, SPRING_BOOT_APPLICATION)
    → MetaAnnotationUtil.getChildren(springBootAppClass, allScope)
  → AnnotatedElementsSearch.searchPsiClasses(annotation, projectScope)
  → toSpringBootApplicationDto(psiClass)
    → SpringBootUtil.getSpringBootStartersInfo(psiClass)
    → SpringBootUtil.getSpringBootVersion(psiClass)
```

**Verdict: STATIC ONLY.** Uses `LibraryClassCache` (library index), `MetaAnnotationUtil` (annotation hierarchy), and `AnnotatedElementsSearch` (PSI index).

**What works without linking:** Finding all `@SpringBootApplication`-annotated classes, their Spring Boot version, starter dependencies, module names, build tools.

**What would be missing:** Nothing. Application class discovery is inherently static — the class either has the annotation or not.

---

## Guard Checks

**No "project not linked" guard exists in any MCP tool path.** Searched for:
- `"not linked"` — only found in `ExternalSystemModule.kt` (unrelated to MCP)
- `"nativeContext"` / `"NativeContext"` — no results
- `isExternalProjectExist()` — used in `SpringSearchServiceFacade`, but **none of the MCP tools use that facade**

The only place where native vs. static routing happens is `SpringSearchServiceFacade`, and MCP tools bypass it entirely.

---

## Architecture Observation

The MCP tools are **intentionally decoupled from native context**. They call `SpringSearchService` directly (or via `McpBeanSearchService` which wraps `SpringSearchService`), never through `SpringSearchServiceFacade`. This means:

1. MCP tools always work, regardless of linking status
2. MCP tools always return static analysis results, even when native context is available
3. There's a potential improvement opportunity: if the project IS linked, MCP tools could provide more accurate bean data by routing through the facade

| Tool | Uses Native Context? | Works Without Linking? | Accuracy Impact |
|------|---------------------|----------------------|-----------------|
| `explyt_find_spring_endpoint` | No | Yes | None — endpoints are annotation-based |
| `explyt_trace_spring_call_chain` | No | Yes | None — traces code structure |
| `explyt_get_project_beans_by_spring_boot_application` | No | Yes | Minor — conditional beans may be inaccurate |
| `explyt_get_spring_boot_applications` | No | Yes | None — app classes are annotation-based |
