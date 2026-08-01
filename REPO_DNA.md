# Repository Engineering DNA Profile (REPO_DNA.md)

This document outlines the engineering patterns discovered in the repository.

## Summary
- **Overall Confidence**: 70.0%
- **Total Discovered Patterns**: 5

## Discovered Patterns

### Exception Naming Suffix (ID: `naming-exception`)
- **Category**: NAMING
- **Description**: Classes representing exceptions should end with the suffix 'Exception'
- **Confidence**: 100.0% (Support: 5, Violations: 0)
- **Stability**: STABLE (Age: 11 commits)

#### Evidence
- `com.repodna.exception.RepoDnaException`: Exception class ends with 'Exception'
- `com.repodna.exception.ValidationException`: Exception class ends with 'Exception'
- `com.repodna.exception.CommandException`: Exception class ends with 'Exception'
- `com.repodna.exception.RepositoryException`: Exception class ends with 'Exception'
- `com.repodna.exception.ConfigurationException`: Exception class ends with 'Exception'

---

### Test Suffix 'Test' (ID: `test-naming-test`)
- **Category**: TESTING
- **Description**: Unit test classes use the 'Test' suffix
- **Confidence**: 89.0% (Support: 8, Violations: 0)
- **Stability**: EMERGING (Age: 11 commits)

#### Evidence
- `com.repodna.scanner.RepositoryScannerTest`: Test ends with Test
- `com.repodna.config.ConfigManagerTest`: Test ends with Test
- `com.repodna.util.DirectoryValidatorTest`: Test ends with Test
- `com.repodna.graph.RepositoryKnowledgeGraphTest`: Test ends with Test
- `com.repodna.dna.DnaEngineTest`: Test ends with Test
- `com.repodna.parser.ParserEngineTest`: Test ends with Test
- `com.repodna.cli.RepoDnaCliTest`: Test ends with Test
- `com.repodna.discovery.PatternDiscoveryEngineTest`: Test ends with Test

---

### Test Suffix 'Tests' (ID: `test-naming-tests`)
- **Category**: TESTING
- **Description**: Unit test classes use the 'Tests' suffix
- **Confidence**: 0.0% (Support: 0, Violations: 8)
- **Stability**: DECLINING (Age: 11 commits)

#### Violations / Outliers
- `com.repodna.scanner.RepositoryScannerTest`: Test ends with Test (violates Tests suffix)
- `com.repodna.config.ConfigManagerTest`: Test ends with Test (violates Tests suffix)
- `com.repodna.util.DirectoryValidatorTest`: Test ends with Test (violates Tests suffix)
- `com.repodna.graph.RepositoryKnowledgeGraphTest`: Test ends with Test (violates Tests suffix)
- `com.repodna.dna.DnaEngineTest`: Test ends with Test (violates Tests suffix)
- `com.repodna.parser.ParserEngineTest`: Test ends with Test (violates Tests suffix)
- `com.repodna.cli.RepoDnaCliTest`: Test ends with Test (violates Tests suffix)
- `com.repodna.discovery.PatternDiscoveryEngineTest`: Test ends with Test (violates Tests suffix)

---

### Package Dependency Boundaries (ID: `package-coupling-boundaries`)
- **Category**: DEPENDENCY
- **Description**: Explicit dependency boundaries verified between package namespaces
- **Confidence**: 63.0% (Support: 8, Violations: 3)
- **Stability**: EXPERIMENTAL (Age: 11 commits)

#### Evidence
- `com.repodna.parser`: Package com.repodna.parser depends on com.repodna.parser.model (66 links)
- `com.repodna.scanner`: Package com.repodna.scanner depends on com.repodna.scanner.model (20 links)
- `com.repodna.scanner`: Package com.repodna.scanner depends on com.repodna.util (1 links)
- `com.repodna.scanner`: Package com.repodna.scanner depends on com.repodna.scanner.detector (6 links)
- `com.repodna.commands`: Package com.repodna.commands depends on com.repodna.config (2 links)
- `com.repodna.commands`: Package com.repodna.commands depends on com.repodna.discovery.model (41 links)
- `com.repodna.commands`: Package com.repodna.commands depends on com.repodna.scanner.model (18 links)
- `com.repodna.commands`: Package com.repodna.commands depends on com.repodna.util (2 links)
- `com.repodna.commands`: Package com.repodna.commands depends on com.repodna.version (6 links)
- `com.repodna.commands`: Package com.repodna.commands depends on com.repodna.dna (19 links)
- `com.repodna.scanner.detector`: Package com.repodna.scanner.detector depends on com.repodna.scanner.model (2 links)
- `com.repodna.discovery`: Package com.repodna.discovery depends on com.repodna.discovery.model (57 links)
- `com.repodna.discovery`: Package com.repodna.discovery depends on com.repodna.graph (13 links)
- `com.repodna.discovery`: Package com.repodna.discovery depends on com.repodna.discovery.detectors (4 links)
- `com.repodna.graph`: Package com.repodna.graph depends on com.repodna.parser (1 links)
- `com.repodna.graph`: Package com.repodna.graph depends on com.repodna.parser.model (51 links)
- `com.repodna.graph`: Package com.repodna.graph depends on com.repodna.graph.model (97 links)
- `com.repodna.graph`: Package com.repodna.graph depends on com.repodna.scanner.model (21 links)
- `com.repodna.dna`: Package com.repodna.dna depends on com.repodna.parser (1 links)
- `com.repodna.dna`: Package com.repodna.dna depends on com.repodna.discovery.model (67 links)
- `com.repodna.dna`: Package com.repodna.dna depends on com.repodna.scanner (1 links)
- `com.repodna.dna`: Package com.repodna.dna depends on com.repodna.scanner.model (7 links)
- `com.repodna.dna`: Package com.repodna.dna depends on com.repodna.graph.model (5 links)
- `com.repodna.dna`: Package com.repodna.dna depends on com.repodna.discovery (1 links)
- `com.repodna.dna`: Package com.repodna.dna depends on com.repodna.graph (4 links)
- `com.repodna.discovery.detectors`: Package com.repodna.discovery.detectors depends on com.repodna.graph.model (53 links)
- `com.repodna.discovery.detectors`: Package com.repodna.discovery.detectors depends on com.repodna.graph (18 links)

#### Violations / Outliers
- `com.repodna.commands`: Package is highly coupled with 6 other packages
- `com.repodna.graph`: Package is highly coupled with 4 other packages
- `com.repodna.dna`: Package is highly coupled with 7 other packages

---

### Acyclic Class Dependency Graph (ID: `circular-dependencies`)
- **Category**: DEPENDENCY
- **Description**: Classes do not participate in circular dependencies
- **Confidence**: 100.0% (Support: 1, Violations: 0)
- **Stability**: STABLE (Age: 11 commits)

#### Evidence
- `graph`: No circular dependencies detected

---

