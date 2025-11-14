Thank you for investing your time and effort in contributing to my project, we appreciate it a lot! 🤗

**TL;DR:** Please ensure:
- All tests pass: `make build`
- Code is clean and well-documented
- Tests are covering new functionality


# General Guidelines

- **Open an Issue**: If you want to contribute a bug fix or a new feature that isn't listed in the [issues](https://github.com/kotlin/kotlinx-schema/issues) yet, please open a new issue for it. We will prioritize it shortly.
- **Follow Best Practices**: Adhere to [Google's Best Practices for Java Libraries](https://jlbp.dev/) and [Google Engineering Practices](https://google.github.io/eng-practices/).
- **Java Compatibility**: Ensure the code is compatible with Java 17.
- **Dependency Management**: Avoid adding new dependencies wherever possible (new dependencies with test scope are OK).
- Adhere to [S.O.L.I.D. Principles](https://en.wikipedia.org/wiki/SOLID) principles in your code.
- **Testing**: Write unit and/or integration tests for your code. This is critical: no tests, no review! Tests should be designed in a way to run in parallel.
- **Run All Tests**: Make sure you run all tests on all modules with `make build`.
- **Maintain Backward Compatibility**: Avoid making breaking changes. Always keep backward compatibility in mind. For example, instead of removing fields/methods/etc, mark them `@Deprecated` and make sure they still work as before.
- **Naming Conventions**: Follow existing naming conventions.
- **Documentation**: Add KDoc where necessary, but the code should be self-documenting.
- **Code Style**: Follow the official Kotlin code style. Use `make format` for automatic code formatting.
- **Discuss Large Features**: Large features should be discussed with maintainers before implementation.
- **Thread Safety**: Ensure that the code you write is thread-safe.

## Using the Makefile

The [`Makefile`](Makefile) includes several helpful targets to make your development process more efficient:

- **`build`**:
  - Runs all tests, verifies the project, and generates site documentation.

- **`apidocs`**:
  - Generates API documentation using Dokka and places it in `target/docs/api`.

- **`lint`**:
  - Prepares the environment and checks code style using `ktlint` and Maven `spotless:check`.

- **`format`**:
  - Formats the code using `ktlint --format` and applies `spotless` and OpenRewrite best practices.

- **`prepare`**:
  - Installs `ktlint` via Homebrew if not already installed.

- **`all`**:
  - Executes `format`, `lint`, and `build` sequentially.
