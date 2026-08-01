package com.repodna.parser.model;

import java.util.List;

/**
 * Immutable model grouping parsed Java types by package namespace.
 */
public record PackageModel(
    String name,
    List<ClassModel> classes,
    List<InterfaceModel> interfaces,
    List<EnumModel> enums,
    List<RecordModel> records
) {}
