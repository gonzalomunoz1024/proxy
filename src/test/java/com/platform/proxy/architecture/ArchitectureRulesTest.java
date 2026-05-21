package com.platform.proxy.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the CQRS Hexagonal architecture and naming conventions from the PRD.
 */
class ArchitectureRulesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.platform.proxy");
    }

    @Test
    void useCasesMustNotCallExternalApisDirectly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..usecases..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework.web.reactive.function.client..");
        rule.check(classes);
    }

    @Test
    void useCasesMustNotDependOnAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..usecases..")
                .should().dependOnClassesThat().resideInAPackage("..adapters..");
        rule.check(classes);
    }

    @Test
    void portsMustBeInterfaces() {
        ArchRule rule = classes()
                .that().resideInAPackage("..ports..").and().haveSimpleNameEndingWith("Port")
                .should().beInterfaces();
        rule.check(classes);
    }

    @Test
    void portNamingConvention() {
        ArchRule rule = classes()
                .that().resideInAPackage("..ports..").and().areInterfaces()
                .should().haveSimpleNameEndingWith("Port");
        rule.check(classes);
    }

    @Test
    void inboundAdaptersResideInInboundPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameContaining("InboundAdapter")
                .should().resideInAPackage("..adapters.inbound..");
        rule.check(classes);
    }

    @Test
    void outboundAdaptersResideInOutboundPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Adapter")
                .and().haveSimpleNameNotContaining("Inbound")
                .should().resideInAPackage("..adapters.outbound..");
        rule.check(classes);
    }

    @Test
    void useCaseImplementationsFollowDefaultNamingConvention() {
        ArchRule rule = classes()
                .that().resideInAPackage("..usecases..")
                .and().areNotInterfaces()
                .and().haveSimpleNameEndingWith("UseCase")
                .should().haveSimpleNameStartingWith("Default");
        rule.check(classes);
    }

    @Test
    void domainMustNotDependOnAdaptersOrUseCases() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..querytranslator.domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..adapters..", "..usecases..");
        rule.check(classes);
    }
}
