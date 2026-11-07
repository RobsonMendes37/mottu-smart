package com.mottu.ops.domain;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.fail;

class DomainPurityTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("com.mottu.ops");

    @Test
    @DisplayName("Hexagonal Law 2: Domain layer must not depend on Spring Framework")
    void domainMustNotDependOnSpring() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.mottu.ops.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "org.springframework.boot.."
                )
                .because("Law 2: com.mottu.ops.domain must be pure Java 21 without Spring dependencies");

        rule.check(classes);
    }

    @Test
    @DisplayName("Hexagonal Law 2: Domain layer must not depend on Jakarta or Javax Persistence (JPA)")
    void domainMustNotDependOnJpa() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.mottu.ops.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..",
                        "javax.persistence..",
                        "jakarta.transaction.."
                )
                .because("Law 2: com.mottu.ops.domain must be pure Java 21 without JPA dependencies");

        rule.check(classes);
    }

    @Test
    @DisplayName("Source Scan: Domain Java source files must not contain prohibited import statements")
    void domainSourceFilesMustNotContainProhibitedImports() throws IOException {
        Path domainDir = Paths.get("src/main/java/com/mottu/ops/domain");
        if (!Files.exists(domainDir)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(domainDir)) {
            List<Path> javaFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();

            for (Path javaFile : javaFiles) {
                List<String> lines = Files.readAllLines(javaFile);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (line.startsWith("import org.springframework.") ||
                        line.startsWith("import org.springframework.boot.") ||
                        line.startsWith("import jakarta.persistence.") ||
                        line.startsWith("import javax.persistence.") ||
                        line.startsWith("import jakarta.transaction.")) {
                        fail("Prohibited import in " + javaFile + " line " + (i + 1) + ": " + line);
                    }
                }
            }
        }
    }
}
