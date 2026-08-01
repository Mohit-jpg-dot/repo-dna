# Project Engineering Rules (PROJECT_RULES.md)

This file lists the enforceable engineering rules learned from this repository.

## Rule 1: Exception Naming Suffix
- **Enforcement Category**: NAMING
- **Rule Statement**: Classes representing exceptions should end with the suffix 'Exception'
- **Confidence Threshold**: 100.0%
- **Violations Allowed**: No

## Rule 2: Test Suffix 'Test'
- **Enforcement Category**: TESTING
- **Rule Statement**: Unit test classes use the 'Test' suffix
- **Confidence Threshold**: 89.0%
- **Violations Allowed**: No

## Rule 3: Acyclic Class Dependency Graph
- **Enforcement Category**: DEPENDENCY
- **Rule Statement**: Classes do not participate in circular dependencies
- **Confidence Threshold**: 100.0%
- **Violations Allowed**: No

