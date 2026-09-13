package com.taca.paymentwallet;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ApplicationLayerArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("com.taca.paymentwallet");

    @Test
    void applicationShouldNotDependOnPresentation() {
        noClasses()
                .that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..presentation..")
                .check(classes);
    }

    @Test
    void applicationShouldNotDependOnInfrastructure() {
        noClasses()
                .that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .check(classes);
    }

    @Test
    void applicationShouldNotDependOnSpring() {
        noClasses()
                .that()
                .resideInAPackage("..application..")
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
    void applicationShouldNotDependOnKafkaOrHttpClient() {
        noClasses()
                .that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.apache.kafka..",
                        "org.springframework.kafka..",
                        "org.springframework.web.client..",
                        "org.springframework.web.reactive.function.client.."
                )
                .check(classes);
    }
}