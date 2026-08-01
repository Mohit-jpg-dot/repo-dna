# Project Architecture Overview (ARCHITECTURE.md)

## Architectural Integrity
- **Overall Architecture Grade**: F (Score: 45/100)
- **Architecture Drift**: 22%

## Health Dimension Scores
- **Architecture Boundary**: 63.0/100 (B)
- **Dependency Integrity**: 100.0/100

## Mermaid Component/Layer Diagram

```mermaid
graph TD
    discovery --> detectors
    discovery --> model
    detectors --> graph
    detectors --> model
    scanner --> model
    graph --> model
    dna --> scanner
    dna --> parser
    dna --> model
    dna --> graph
    dna --> discovery
    commands --> util
    commands --> dna
    commands --> model
    detector --> model
    parser --> model
    graph --> parser
    scanner --> util
    scanner --> detector
    discovery --> graph
    commands --> config
    commands --> version
```
