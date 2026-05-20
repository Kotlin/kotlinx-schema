# Contributing Guidelines

Thank you for investing your time and effort in contributing to this project, we appreciate it a lot! 🤗

There are two main ways to contribute to the project &mdash; submitting issues and submitting
fixes/changes/improvements via pull requests.

## Submitting issues

Both bug reports and feature requests are welcome.
Submit issues [here](https://github.com/Kotlin/kotlinx-schema/issues).

* Search for existing issues to avoid reporting duplicates.
* When submitting a bug report:
    * Use a 'bug report' template when creating a new issue.
    * Test it against the most recently released version. It might have been already fixed.
    * By default, we assume that your problem reproduces in Kotlin/JVM. Please, mention if the problem is
      specific to Kotlin/JS, Kotlin/Wasm or Kotlin/Native.
    * Include the code that reproduces the problem. Provide the complete reproducer code, yet minimize it as much as possible.
    * However, don't put off reporting any weird or rarely appearing issues just because you cannot consistently
      reproduce them.
    * If the bug is in behavior, then explain what behavior you've expected and what you've got.
* When submitting a feature request:
    * Use a 'feature request' template when creating a new issue.
    * Explain why you need the feature &mdash; what's your use-case, what's your domain.
    * Explaining the problem you face is more important than suggesting a solution.
      Even if you don't have a proposed solution, please report your problem.
    * If there is an alternative way to do what you need, then show the code of the alternative.

## Submitting PRs

We love PRs. Submit PRs [here](https://github.com/Kotlin/kotlinx-schema/pulls).
However, please keep in mind that maintainers will have to support the resulting code of the project,
so do familiarize yourself with the following guidelines.

* All development (both new features and bug fixes) is performed in the `main` branch.
    * Create your feature branch from `main`.
    * Base PRs against the `main` branch.
    * Documentation-only changes can be submitted as PRs to `main`, unless the documentation is in source code where the patch changes line numbers.
* If you plan extensive rewrites/additions to the docs, then please [contact the maintainers](#contacting-maintainers) to coordinate the work in advance.
* If you make any code changes:
    * Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/reference/coding-conventions.html).
        * Use 4 spaces for indentation.
        * Do not use imports with '*'.
    * **Make sure your code is covered with tests!**
    * Keep code clean and well-documented.
    * [Build the project](#building-and-testing) to make sure it all works and passes the tests.
* If you fix a bug:
    * First, write the test that reproduces the bug.
    * Fixes without tests are accepted only in exceptional circumstances if it can be shown that writing the
      corresponding test is too hard or otherwise impractical.
    * Follow the style of writing tests that is used in this project.
* If you introduce any new public APIs:
    * All new APIs must come with documentation and tests.
    * If you plan large API additions, then please start by submitting an issue with the proposed API design
      to gather community feedback.
    * [Contact the maintainers](#contacting-maintainers) to coordinate any big piece of work in advance.
* Comment on the existing issue if you want to work on it. Ensure that the issue not only describes a problem,
  but also describes a solution that has received positive feedback. Propose a solution if there isn't any.


## General Guidelines

- **Java Compatibility**: Ensure the code is compatible with Java 17.
- **Dependency Management**: Avoid adding new dependencies wherever possible (new dependencies with test scope are OK).
- **Testing**: Write unit and/or integration tests for your code. This is critical: no tests, no review! Tests should be designed in a way to run in parallel.
- **Run All Tests**: Make sure you run all tests on all modules with `./gradlew build`.
- **Maintain Backward Compatibility**: Avoid making breaking changes. Always keep backward compatibility in mind. For example, instead of removing fields/methods/etc, mark them `@Deprecated` and make sure they still work as before.
- **Naming Conventions**: Follow existing naming conventions.
- **Documentation**: Add KDoc where necessary, but the code should be self-documenting.
- **Code Style**: Follow the official Kotlin code style.
- **Discuss Large Features**: Large features should be discussed with maintainers before implementation.
- **Thread Safety**: Ensure that the code you write is thread-safe.

## Building and Testing

### Quick Commands

```bash
# Fetch submodules
git submodule update --init --recursive --depth=1

# Full build + tests (matches CI). Run before commit.
./gradlew build

# ABI check — fails if api/*.api is stale
./gradlew checkKotlinAbi

# Verify README/docs code snippets
./gradlew :docs:clean knit knitCheck --no-configuration-cache
./gradlew :docs:test --rerun-tasks

# Generate API docs (→ docs/public/apidocs)
./gradlew clean :docs:dokkaGenerate

# Regenerate ABI dump after public API changes; commit the api/*.api diffs
./gradlew updateKotlinAbi
```

### Testing Specific Modules

```bash
# KSP processor tests (no Gradle plugin)
./gradlew :ksp-integration-tests:test

# Gradle plugin integration tests (separate composite build).
# Requires the plugin to be published to build/project-repo first:
./gradlew publishAllPublicationsToProjectRepository -Pversion=1-SNAPSHOT --rerun-tasks
(cd gradle-plugin-integration-tests && ./gradlew build -PkotlinxSchemaVersion=1-SNAPSHOT)

# Or use the convenience tasks:
./gradlew publishPluginAndSync   # publishes plugin, then reload IDE
./gradlew testGradlePlugin       # publishes + runs :gradle-plugin-integration-tests:allTests
```

**How gradle plugin is tested**: `gradle-plugin-integration-tests` is an independent build
that includes the main build via [`includeBuild()`](https://docs.gradle.org/current/userguide/organizing_gradle_projects.html#sec:composite_builds).
It applies the plugin to a real multiplatform project, runs KSP, and verifies generated schemas.
This tests the plugin exactly as external users would use it,
without requiring maven publication during development.

Generated KSP sources land in `<module>/build/generated/ksp/metadata/commonMain/kotlin`.

## Contacting maintainers

If something cannot be done, not convenient, or does not work, please [submit an issue](https://github.com/Kotlin/kotlinx-schema/issues).
