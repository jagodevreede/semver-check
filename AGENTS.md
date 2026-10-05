# AGENTS.md - Guidelines for Agentic Coding Agents

This document provides guidelines and reference information for agentic coding agents operating in this repository.

## Project Overview

This is a Maven-based Java project (Java 11+) that implements a semantic versioning check plugin. The core functionality analyzes JAR files to determine appropriate SemVer updates based on API changes.

## Build, Lint, and Test Commands

### Standard Maven Commands
- **Build:** `mvn clean install`
- **Run tests:** `mvn test`
- **Run with coverage:** `mvn verify -Pcoverage`
- **Skip tests:** `mvn install -DskipTests`
- **Compile only:** `mvn compile`

### Running a Single Test (Recommended)
```bash
# Run specific test class
mvn test -Dtest=SemVerCheckerSimpleTest

# Run specific test method
mvn test -Dtest=SemVerCheckerSimpleTest#determineSemVerType_sameJarShouldResultInNoChange

# Run with debug output
mvn test -X
```

### Test Framework Details
- **Framework:** JUnit 5 (Jupiter)
- **Assertions:** AssertJ fluent assertions
- **Mocking:** Mockito with Mockito-Junit-jupiter extension
- **Parameterized tests:** Supported via `@ParameterizedTest` and various providers

## Code Style Guidelines

### Imports and Ordering
1Static imports last (placed at bottom)
2. Use wildcard imports for convenience when appropriate

### Naming Conventions
- **Classes:** PascalCase (e.g., `SemVerChecker`, `Configuration`)
- **Methods:** camelCase (e.g., `determineSemVerType`, `getIncludePackages`)
- **Fields:** camelCase with descriptive names (e.g., `annotationAddedStrategy`)
- **Constants:** UPPER_SNAKE_CASE with `static final` modifier
- **Parameters:** camelCase, no prefixes/suffixes

### Code Formatting
- Indentation: 4 spaces
- Line length: Maximum 150 characters preferred
- Braces: K&R style (opening brace on same line)
- Empty lines: One between methods, minimal within methods

### Type Usage
- Prefer concrete types over interfaces when implementation is clear
- Use `List`, `Set`, `Map` for collection declarations
- Avoid raw types; always use generics
- Use `Optional` for return values that may be absent
- Final variables where possible

### Error Handling
1. **Checked exceptions:** Declare in method signature (`throws IOException`)
2. **Runtime exceptions:** Use for programming errors or unexpected conditions
3. **Custom exceptions:** Create specific exception types (e.g., `HaltException`)
4. **Logging:** Use SLF4J with appropriate log levels:
   - `log.debug()`: Detailed debugging information
   - `log.info()`: Significant events and state changes
   - `log.warn()`: Recoverable errors or warnings
   - `log.error()` or `log.trace()`: Errors and trace-level details

### Annotations
- Use `@SuppressWarnings` sparingly with specific warnings
- Maven plugin annotations (`@Mojo`, `@Parameter`) for plugin development
- JUnit 5 annotations: `@Test`, `@BeforeEach`, `@AfterEach`, `@ParameterizedTest`, `@CsvSource`
- `@Inject` for dependency injection in Maven plugins

### Documentation Style
1. **Javadoc:** Required for public classes, methods, and fields
2. **Format:** Standard Javadoc with descriptive paragraphs
3. **Tags:** Use `@param` for parameters, `@return` for return values, `@throws` for exceptions
4. **Inline comments:** Avoid; prefer self-documenting code

### Testing Best Practices
- Test class names end with `Test` suffix (e.g., `SemVerCheckerSimpleTest`)
- Use descriptive test method names: `methodName_expectation_descriptiveCondition()`
- Arrange-Act-Assert pattern for test structure
- Mock external dependencies using Mockito
- Use AssertJ for fluent assertions
- Parameterized tests for multiple input scenarios

### Project Structure
```
semver-check-core/          # Core library (JAR comparison logic)
  src/main/java/...         # SemVerChecker, Configuration, etc.
  src/test/java/...         # Unit tests with JUnit 5

semver-check-maven-plugin/  # Maven plugin implementation
  src/main/java/...         # SemVerMojo, HaltException
  src/test/java/...         # Integration tests

sample/                     # Sample projects for testing
```

### Git Workflow
- Follow Conventional Commits specification for commit messages
- Create feature branches from main
- Submit pull requests for all changes
