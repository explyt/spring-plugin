## Description

During the Spring vs Ktor benchmark preparation, several repeated footguns were discovered that could be caught by inspections.

Suggested inspections:

1. Docker Compose hardcoded platform mismatch:

   ```yaml
   platform: linux/arm64
   ```

   while target VPS is `x86_64`.

2. Source Dockerfile changed but compose still points to old remote image tag.

   Example: fixed Dockerfile entrypoint, but compose still used `imuromtsev/bench-ktor-netty:1.0.0` with old entrypoint.

3. Docker healthcheck uses `wget`/`curl`, but runtime image does not install that binary.

4. k6 Rate metric parsed from wrong field:

   ```text
   error_rate read from .rate instead of .value
   ```

5. k6 thresholds abort benchmark exploration unless runner handles exit code 99.

6. Timeout plateau detection:

   p95/p99 flat near 5000/7000/30000 ms should be flagged as timeout artifact.

7. Ktor JVM option configured in compose but not consumed by Docker entrypoint:

   ```text
   KTOR_JVM_OPTS set, but ENTRYPOINT only uses $JAVA_OPTS
   ```

8. Spring MVC + Virtual Threads with `StructuredTaskScope` requires `--enable-preview` while JEP 505 is still preview.

9. Mutable fields in `@RestController` singleton can be shared mutable state.

## Additional context

These came from real failures during a Kotlin Meetup benchmark:

- WireMock OOM due request journal;
- Ktor timeout plateau;
- stale Docker images after source fixes;
- missing healthcheck binaries;
- k6 error rate parsing bug;
- VT preview flag missing.

Observed in plugin version: `5.8.1-IJ-261-nightly-260421`.

## Customer company name / Competitors / Target Audience

**Competitors:** These are domain-specific inspections; no direct competitor reference. They are similar in spirit to IDE inspections for Dockerfile/Compose/spring configuration mistakes.

**Target audience:** Developers running benchmark/demo environments, Spring/Ktor users, Kotlin backend developers, plugin dogfooding scenarios.

## Issue checklist

- [x] I've chosen the right label for issue (scope..., env..., improves...)