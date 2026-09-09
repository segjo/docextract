package ch.adeon.apps.docextract.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HexagonalArchitectureTest {

  private static final String BASE = "ch.adeon.apps.docextract";

  /** Verbindliche Modulliste (copilot-instructions.md §5 / ARCHITECTURE.md §5.3). */
  private static final List<String> FEATURE_MODULES =
      List.of(
          "ingest",
          "structuring",
          "retrieval",
          "extraction",
          "validation",
          "agentgateway",
          "process",
          "audit",
          "security");

  private static JavaClasses productionClasses;

  @BeforeAll
  static void importProductionClasses() {
    productionClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);
  }

  @Test
  void classes_reside_only_in_declared_modules() {
    String[] declaredPackages =
        Stream.concat(FEATURE_MODULES.stream(), Stream.of("shared"))
            .map(module -> BASE + "." + module + "..")
            .toArray(String[]::new);

    classes()
        .that()
        .resideOutsideOfPackage(BASE)
        .should()
        .resideInAnyPackage(declaredPackages)
        .check(productionClasses);
  }

  @Test
  void feature_module_classes_reside_in_domain_application_or_adapter() {
    String[] featurePackages =
        FEATURE_MODULES.stream().map(module -> BASE + "." + module + "..").toArray(String[]::new);

    classes()
        .that()
        .resideInAnyPackage(featurePackages)
        .should()
        .resideInAnyPackage("..domain..", "..application..", "..adapter..")
        .check(productionClasses);
  }

  @Test
  void domain_must_not_depend_on_spring() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework..")
        .check(productionClasses);
  }

  @Test
  void domain_must_not_depend_on_application_or_adapter() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..application..", "..adapter..")
        .check(productionClasses);
  }

  @Test
  void application_must_not_depend_on_adapter() {
    noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..adapter..")
        .check(productionClasses);
  }

  @Test
  void adapters_are_not_reachable_from_other_modules() {
    // Cross-Modul-Aufrufe laufen ausschliesslich über Ports, nie Adapter zu Adapter.
    FEATURE_MODULES.forEach(
        module ->
            noClasses()
                .that()
                .resideOutsideOfPackage(BASE + "." + module + "..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage(BASE + "." + module + ".adapter..")
                .as("no class outside of module '%s' may depend on its adapters".formatted(module))
                .check(productionClasses));
  }
}
