# AGENTS: Development Guidelines for AI Contributors

This document is for autonomous agents and AI copilots contributing code to this repository. Follow these rules to keep
changes safe, comprehensible, and easy to maintain.

## Prime directives

1. Tests first, always.
    - Before changing code, identify or add tests that express the desired behavior.
    - Prefer readable, minimal tests over clever ones. Tests are documentation.
2. Keep tests simple and explicit.
    - Arrange/Act/Assert structure; avoid hidden magic and overuse of helpers.
    - Prefer concrete inputs/outputs; avoid randomness and time dependence.
3. Uphold SOLID principles in production code.
    - Single Responsibility: each class/function should do one thing well.
    - Open/Closed: extend via new code, avoid risky edits to stable code paths.
    - Liskov Substitution: honor contracts; keep types substitutable.
    - Interface Segregation: keep abstractions small and focused.
    - Dependency Inversion: depend on abstractions, not concretions.
4. Make the minimal change that satisfies the tests and the issue.
5. Keep the build green. Do not merge changes that break existing tests.
6. Prefer clarity over micro-optimizations and cleverness.
7. Ask when uncertain. If requirements are ambiguous, request clarification with a concise question.
8. Write code with the quality of a Kotlin Champion.

## Code Style

### Kotlin

- Follow Kotlin coding conventions
- Use the provided `.editorconfig` for consistent formatting
- Use Kotlin typesafe DSL builders where possible and prioritize fluent builders style over standard builder methods.
  If DSL builders produce less readable code, use standard setter methods.
- Prefer DSL builder style (method with lambda blocks) over constructors, if possible.
- Use Kotlin's `val` for immutable properties and `var` for mutable properties. Consider using `lateinit var` instead of
  nullable types, if possible.
- Use multi-dollar interpolation prefix for strings, where applicable
- Use fully qualified imports instead of star imports
- Ensure to preserve backward compatibility when making changes

## Testing guidance

- Write comprehensive tests for new features
- **Prioritize test readability**
- Avoid creating too many test methods. If multiple parameters can be tested in one scenario, go for it. Consider using
  parametrized tests.
- When running tests on a Kotlin Multiplatform project, run only JVM tests,
  if no asked to run tests on another platform too.
- Use function `Names with backticks` for test methods in Kotlin, e.g. "fun `should return 200 OK`()"
- Avoid writing KDocs for tests, keep code self-documenting
- Use Kotest JSON assertions for json and json schema comparisons:
    - Example: `schema shouldEqualJson """ { ... } """.trimIndent()`
    - For Kotlin raw strings containing JSON Schema keywords starting with `$`, escape with Kotlin interpolation escape
      in tests: use $$""" and `${'$'}` inside raw strings where needed, e.g. `${'$'}id`, `${'$'}defs`, `${'$'}ref`.
- Prefer verifying both forms where applicable:
    - `KClass<T>.jsonSchemaString`
    - `KClass<T>.jsonSchema` (JsonObject parsed from the string)
- Keep test JSON readable:
    - Use the `// language=json` comment before multiline JSON blocks for IDE support.
    - Avoid brittle whitespace assertions; compare by structure using `shouldEqualJson`.
- Cover typical scenarios when modifying the generator/introspector:
    - Primitives, enums, nullable properties, lists/maps, nested objects, generics (star-projection to kotlin.Any), and
      description propagation from @Description.
- Ensure non-annotated classes do not gain generated extensions.
- Write Kotlin tests with [kotlin-test](https://github.com/JetBrains/kotlin/tree/master/libraries/kotlin.test),
  [mockk](https://mockk.io/) and [Kotest-assertions](https://kotest.io/docs/assertions/assertions.html)
  with infix form assertions `shouldBe` instead of `assertEquals`.
- Use Kotest's `withClue("<failure reason>")` to describe failure reasons, but only when the assertion is NOT obvious.
  Remove obvious cases for simplicity.
- When making multiple assertions against a nullable field, use the Kotest lambda-style `shouldNotBeNull { ... }` 
  extension to both assert non-nullability and provide a scope for further assertions on the receiver.
- Avoid using `this.` explicitly inside lambda blocks (like `shouldNotBeNull`, `assertSoftly`, or DSL builders) 
  unless necessary to resolve shadowing or ambiguity.
  - Example:
    ```kotlin
    prop.shouldNotBeNull {
        enum shouldHaveSize 2 // Preferred
        this.enum shouldHaveSize 2 // Avoid "this." if possible
    }
    ```
- Use `assertSoftly(subject) { ... }` to perform multiple assertions. Never use `assertSoftly { }` to verify properties
  of different subjects, or when there is only one assertion per subject. Avoid using `assertSoftly(this) { ... }`
- When asked to write tests in Java: use JUnit5, Mockito, AssertJ core

## Coding guidance (apply SOLID pragmatically)

- Respect module boundaries and separation of concerns:
    - Introspection (KSP) should only gather model metadata (no JSON specifics).
    - IR (SchemaIR) describes types; keep it framework-agnostic.
    - Emitters convert IR to concrete schema (JSON Schema). Avoid KSP/serialization leakage here.
- Propagate descriptions consistently:
    - Class-level and property-level `@Description` must appear as `description` in the emitted schema.
- Handle nullability and references as established:
    - Primitives: `type: ["<type>", "null"]` for nullable.
    - Refs: use `oneOf: [ { "$ref": "#/$defs/Type" }, { "type": "null" } ]` when nullable.
    - Root schema layout uses `$id`, `$defs`, and `$ref`.
- Keep changes small and reversible. Prefer adding small functions over editing many call sites.
- Write self-explanatory code; add focused comments where intent is non-obvious.
- use `git mv` command when moving version-controlled files. Never commit and/or push changes yourself.

## Workflow for AI agents

1. Understand the issue.
    - Summarize the requirement in one or two sentences.
    - Identify affected files and tests. If unclear, ask.
2. Plan minimal changes and update the plan using the provided status tool.
3. When viewing, editing, and running tests prefer jetbrains MCP server, if it's available.
4. Start with tests.
    - Update existing tests or add new ones in `ksp-integration-tests` or relevant module tests.
    - Keep tests deterministic and small.
5. Implement the change.
    - Honor SOLID; prefer composition and small functions.
    - Avoid breaking public APIs without tests and discussion.
6. Run tests locally:
    - `./gradlew test`
    - KSP integration tests: `./gradlew :ksp-integration-tests:test`
    - To trigger KSP codegen: `./gradlew :ksp-integration-tests:build` and inspect
      `ksp-integration-tests/build/generated/ksp/`.
7. Verify schemas by structure, not whitespace. Use Kotest JSON matchers.
8. Keep or improve readability of both tests and code.
9. Document key decisions briefly in PR description or commit message.

## PR/test checklist

- Tests clearly describe the behavior and are easy to read.
- Tests pass across all targets configured by CI.
- No unnecessary complexity added; change is minimal and focused.
- Schema assertions use `shouldEqualJson` with properly escaped `$` keys.
- New behaviors are covered for both `jsonSchemaString` and `jsonSchema`.
- Non-annotated classes remain without generated extensions.
- Code follows SOLID and module boundaries.

### Documentation

- Never make up the documentation/facts.
- Update README files when adding new features
- Document API changes in the appropriate module's documentation
- **Keep documentation concise and straight to the point.**
- Always verify that documentation matches the code and is always truthful. Fix discrepancies between code and
  documentation.
- Write tutorial in Markdown format in README.md
- Make sure that in production code interfaces and abstract classes are properly documented. Avoid adding KDocs to
  override functions to avoid verbosity.
- Update KDocs when api is changed.
- When referring classes in KDoc, use references: `[SendMessageRequest]` instead of `SendMessageRequest`.
- Add brief code examples to KDoc
- Add links to specifications, if known. Double-check that the link actual and pointing exactly to the specification.
  Never add broken or not accurate links.

## Build and run commands

- Build all modules: `./gradlew build`
- Run all tests: `./gradlew test`
- KSP integration tests: `./gradlew :ksp-integration-tests:test`

## When to ask for help

- Requirements conflict with existing tests or documentation.
- The smallest change still requires altering core abstractions.
- Unclear expected schema shape for a new feature—ask for a concrete, readable test case to anchor implementation.
