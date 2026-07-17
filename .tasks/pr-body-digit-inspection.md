<!--
  Copyright (c) 2024 Explyt Ltd
  SPDX-License-Identifier: Apache-2.0
-->

## Summary

Fixes false-positive `Cannot resolve key property` warnings on YAML list/map element keys when an ancestor property name contains a digit followed by an uppercase letter (e.g. Kotlin/Java property `s3Logs` written canonically as `s3-logs` in `application.yml`).

Real-world repro: `@ConfigurationProperties(prefix = "explyt.ingest")` with `s3Logs.sources: List<S3Source>` — every `explyt.ingest.s3-logs.sources[N].<field>` key was flagged, while the properties bind correctly at runtime.

Two cooperating defects, both fixed:

1. **`RenameUtil.isUnderscoreRequired`** required a *lowercase* char before an uppercase to insert a dash, so `s3Logs` canonicalized to `s3logs` instead of `s3-logs`. Spring Boot's own `ConventionUtils.toDashedCase` inserts the dash after digits too. Now a digit is accepted as the preceding char.
2. **`SpringBasePropertyInspection.getListKeys`/`getMapKeys`** matched the file key against model names with a raw `startsWith`, unlike the rest of the resolution chain which compares via relaxed `PropertyUtil.toCommonPropertyForm`. This made list/map element resolution brittle to any canonicalization discrepancy — it also flagged list children under literal camelCase YAML keys (e.g. `camelWritten:`), which Spring relaxed binding accepts. Both sides are now compared in common form.

The diagnostic signature of the bug: scalar children of the affected key resolved fine (`s3-logs.enabled`), only list/map *element* children were flagged — because only the list/map prefix path used the strict comparison.

Side benefit: `convertSetterToPKebabCase` is shared with `SpringConfigurationPropertyKeyReferenceProvider` and `ConfigurationPropertyPsiElementProcessor`, so key navigation/rename for digit-boundary property names is fixed as well.

## Related issue

n/a

## Type of change

- [x] Bug fix
- [ ] New feature / inspection
- [ ] Documentation
- [ ] Refactoring / tech debt
- [ ] Other (describe below)

## How was this tested?

New tests in both `SpringYamlInspectionTest` twins (Java + Kotlin):

- `testListElementUnderDigitBoundaryKey` — `s3Logs.sources: List<Source>` bound from `s3-logs.sources[0].name`, expects no warning;
- `testListElementUnderNonKebabKey` — literal `camelWritten:` YAML key: list children resolve, only the legitimate `Should be kebab-case` hint remains.

Verified red-green: with the production fix stashed, both new tests fail; with the fix, they pass.

```
./gradlew :spring-core:test --tests "com.explyt.spring.core.inspections.kotlin.SpringYamlInspectionTest" --tests "com.explyt.spring.core.inspections.java.SpringYamlInspectionTest"
```

Also ran the full `*properties*`/`*Yaml*` test slice of `:spring-core` — green. Note: `java.SpringBoot4PropertyMigrationInspectionTest` (3 tests) fails on clean `main` with and without this change — pre-existing, unrelated.

## Checklist

- [x] My branch targets `main`.
- [x] Code is Kotlin-idiomatic and consistent with the surrounding style.
- [x] Every new file has the Apache-2.0 SPDX header.
- [x] I added or updated tests for my change (or explained why not).
- [x] I updated relevant documentation (README / wiki / bundle messages) if behavior changed.
