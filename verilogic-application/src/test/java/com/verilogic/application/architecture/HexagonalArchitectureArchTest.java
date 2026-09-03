package com.verilogic.application.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * High-Assurance Architectural Unit Test enforcing Hexagonal Architecture and SOLID principles.
 */
class HexagonalArchitectureArchTest {

    private final JavaClasses importedClasses = new ClassFileImporter().importPackages("com.verilogic");

    @Test
    @DisplayName("Architectural Invariant: Domain Layer must have zero external framework dependencies")
    void domainMustNotDependOnFrameworks() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta..", "com.vaadin..", "redis.clients..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Architectural Invariant: Application Ports must not depend on Infrastructure Adapters")
    void portsMustNotDependOnAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application.port..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..infrastructure..", "..ui..");

        rule.check(importedClasses);
    }
}
