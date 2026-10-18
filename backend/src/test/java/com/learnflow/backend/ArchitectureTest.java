package com.learnflow.backend;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    @Test
    void controllersNeverDependOnRepositories() {
        ArchRule rule = noClasses()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Repository");
        rule.check(
            new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.learnflow.backend")
        );
    }

    @Test
    void serviceRepositoryDependenciesStayWithinTheirDomain() {
        var classes = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.learnflow.backend"); // Inspect actual dependencies so each domain may use its own repositories only.
        for (var c : classes)
            if (
                c.getSimpleName().endsWith("Service")
            ) for (var dependency : c.getDirectDependenciesFromSelf()) {
                var target = dependency.getTargetClass();
                if (
                    target.getSimpleName().endsWith("Repository")
                ) org.junit.jupiter.api.Assertions.assertEquals(
                    c.getPackageName(),
                    target.getPackageName(),
                    dependency.getDescription()
                );
            }
    }
}
