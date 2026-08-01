# AI Agent Coding Instructions (AGENTS.md)

> **Important**: This repository follows strict engineering rules. Adhere to these patterns when generating code.

## Project Context
- **Project Name**: repo-dna
- **Primary Language**: Java
- **Build System**: Gradle Kotlin DSL

## Core Signatures learned from Repository
- **Exception Naming Suffix**: Classes representing exceptions should end with the suffix 'Exception' (100.0% consistency)
- **Test Suffix 'Test'**: Unit test classes use the 'Test' suffix (89.0% consistency)
- **Acyclic Class Dependency Graph**: Classes do not participate in circular dependencies (100.0% consistency)

## DOs and DONTs
### DO
- End unit test class names with the `Test` suffix.
- Write clean Javadoc documentation on all public API methods.
- Place database entities in `.entity` or `.model` packages.

### DONT
- Do not create exception classes that do not end with the `Exception` suffix.
- Do not introduce circular package dependencies.
- Do not write JUnit tests ending with suffixes other than standard project test conventions.
