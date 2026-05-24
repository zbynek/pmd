# PMD repository instructions

## Build, test, and lint

- Use the Maven wrapper from the repo root: `./mvnw` on Unix-like shells, `.\mvnw.cmd` on Windows.
- The current Maven build requires **JDK 21+**. Some older docs still mention Java 11+, but the root enforcer rule now rejects anything below 21.
- Full local build: `./mvnw clean verify`
- Fast CI-style build when you only need compilation/packaging: `./mvnw verify -PfastSkip -DskipTests`
- Repo linting is part of `verify`: root `verify` runs Checkstyle and PMD/CPD dogfooding. There is no separate top-level lint script to prefer over `verify`.
- Run one module’s tests with its upstream dependencies: `./mvnw -pl pmd-java -am test`
- Run a single test class: `./mvnw -pl pmd-core -am -Dtest=AbstractRuleTest test`
- Run a single test method: `./mvnw -pl pmd-java -am -Dtest=ASTBooleanLiteralTest#methodName test`
- If you change rule docs or ruleset metadata, regenerate/check rule docs with: `./mvnw verify -Pgenerate-rule-docs,fastSkip -DskipTests -Dassembly.skipAssembly=true`

## High-level architecture

- This is a Maven reactor for a **multi-language static analyzer** plus **CPD**. The root `pom.xml` builds shared infrastructure, many `pmd-*` language modules, test utility modules, the CLI, docs tooling, and the distribution package.
- `pmd-core` is the main programmatic API. `PmdAnalysis` owns the end-to-end analysis flow: collect files, load rulesets, configure renderers/listeners, then dispatch work per language.
- `pmd-cli` is a thin CLI layer over `pmd-core`; CLI behavior should generally be implemented by wiring configuration and analysis APIs rather than duplicating analysis logic in the CLI module.
- Languages are discovered from the classpath through `ServiceLoader` in `LanguageRegistry`, then filtered into PMD-capable or CPD-capable registries. Adding a language module is primarily a classpath/service-registration concern, not a hardcoded central registry edit.
- Per-language execution happens through `LanguageProcessor` implementations. The documented flow is: parse input into an AST, build semantic information (symbol table, DFA, type resolution as needed), run rule-chain rules first, then run the remaining AST-walking rules, and finally render/report violations.
- Language-specific analyzers live in modules such as `pmd-java`, `pmd-javascript`, etc. They depend on `pmd-core` and provide parser/AST/rule implementations plus ruleset resources under `src/main/resources/category/<language>/`.
- `pmd-test` is the shared rule-testing framework. `pmd-lang-test` is the shared parser/AST testing utility module and uses Kotlin/Kotest. `pmd-doc` generates and validates rule documentation. `pmd-dist` assembles the shipping distribution ZIPs and runs distribution-level integration tests.

## Key conventions

- Prefer working in the right layer:
  - shared analysis/config/reporting behavior belongs in `pmd-core`
  - command-line argument handling belongs in `pmd-cli`
  - language syntax, AST, and rules belong in the relevant `pmd-<language>` module
- Rule tests follow a repository-specific convention:
  - a rule test class usually extends `PmdRuleTst`
  - the test class name maps to the rule name by stripping the `Test` suffix
  - the package layout determines the ruleset path (`category/<language>/<category>.xml`)
  - XML test data is loaded from the matching test resource location
- For multi-rule XML-based tests, use `SimpleAggregatorTst` and register rules in `setUp()` with `addRule(...)` rather than building ad hoc harnesses.
- Rulesets are treated as versioned resources, not just metadata files. Many language modules enable resource filtering so ruleset XML can use build properties such as `${pmd.website.baseurl}`.
- Do not casually depend on `*.internal` packages across modules. The build explicitly excludes internal packages from published Javadocs and binary-compatibility checks, so those packages are not part of the supported external API surface.
- `verify` is intentionally heavy. The `fastSkip` profile is the standard shortcut when you need a fast compile/package/test loop without javadocs, Checkstyle, PMD/CPD dogfooding, CycloneDX, Dokka, or japicmp.
