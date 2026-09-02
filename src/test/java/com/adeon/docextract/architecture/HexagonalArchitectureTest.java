package com.adeon.docextract.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.importer.ClassFileImporter;

class HexagonalArchitectureTest {

    @Test
    void domain_must_not_depend_on_spring() {
        var classes = new ClassFileImporter().importPackages("com.adeon.docextract");

        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                .check(classes);
    }

    @Test
    void application_must_not_depend_on_adapter() {
        var classes = new ClassFileImporter().importPackages("com.adeon.docextract");

        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage("..adapter..")
                .check(classes);
    }
}
