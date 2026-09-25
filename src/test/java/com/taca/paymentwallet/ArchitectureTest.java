package com.taca.paymentwallet;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class ArchitectureTest {

    private final JavaClasses classes =
            new ClassFileImporter()
                    .withImportOption(
                            ImportOption.Predefined.DO_NOT_INCLUDE_TESTS
                    )
                    .importPackages(
                            "com.taca.paymentwallet"
                    );

    @Test
    void domainShouldNotDependOnSpringOrJpa() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "javax.persistence.."
                )
                .check(classes);
    }

    @Test
    void presentationShouldNotAccessInfrastructureDirectly() {
        noClasses()
                .that()
                .resideInAPackage("..presentation..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .check(classes);
    }

    @Test
    void applicationShouldNotDependOnPresentationOrInfrastructure() {
        noClasses()
                .that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..presentation..",
                        "..infrastructure.."
                )
                .check(classes);
    }

    @Test
    void layeredArchitectureShouldBeRespected() {
        layeredArchitecture()
                .consideringAllDependencies()
                .layer("Presentation").definedBy("..presentation..")
                .layer("Application").definedBy("..application..")
                .layer("Domain").definedBy("..domain..")
                .layer("Infrastructure").definedBy("..infrastructure..")
                .whereLayer("Presentation").mayNotBeAccessedByAnyLayer()
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Presentation", "Infrastructure")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
                .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
                .check(classes);
    }
}
