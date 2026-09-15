package com.ibizabroker.bibliotheque.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.Entity;
import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Règles d'architecture du backend (fitness functions), exécutées avec les
 * tests unitaires. Découpage par fonctionnalité (ADR 0003) :
 *
 * <pre>
 * com.ibizabroker.bibliotheque
 * ├── catalogue | emprunts | reservations | utilisateurs | auth | donnees
 * │   ├── api        ce que la fonctionnalité offre aux autres
 * │   ├── web        contrôleurs et DTO de requête
 * │   └── internal   services, entités, repositories
 * └── shared         configuration, erreurs, filtres web, utilitaires
 * </pre>
 */
@AnalyzeClasses(packages = "com.ibizabroker.bibliotheque", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String RACINE = "com.ibizabroker.bibliotheque.";

    private static final DescribedPredicate<JavaClass> ENTITES_JPA =
            DescribedPredicate.describe("des entités JPA", classe -> classe.isAnnotatedWith(Entity.class));

    private static final DescribedPredicate<List<JavaClass>> UNE_ENTITE_JPA_PARMI_LES_PARAMETRES =
            DescribedPredicate.describe("une entité JPA parmi les paramètres",
                    types -> types.stream().anyMatch(ENTITES_JPA));

    // ------------------------------------------------------------------
    // Frontières entre fonctionnalités
    // ------------------------------------------------------------------

    /** Aucun cycle entre fonctionnalités (ni avec shared). */
    @ArchTest
    static final ArchRule fonctionnalitesSansCycle = slices()
            .matching(RACINE + "(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule catalogueEncapsule = encapsulee("catalogue");

    @ArchTest
    static final ArchRule empruntsEncapsules = encapsulee("emprunts");

    @ArchTest
    static final ArchRule reservationsEncapsulees = encapsulee("reservations");

    @ArchTest
    static final ArchRule utilisateursEncapsules = encapsulee("utilisateurs");

    @ArchTest
    static final ArchRule authEncapsulee = encapsulee("auth");

    @ArchTest
    static final ArchRule donneesEncapsulees = encapsulee("donnees");

    /** shared est la base commune : il ne dépend d'aucune fonctionnalité. */
    @ArchTest
    static final ArchRule sharedIndependant = noClasses()
            .that().resideInAPackage(RACINE + "shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    RACINE + "catalogue..", RACINE + "emprunts..", RACINE + "reservations..",
                    RACINE + "utilisateurs..", RACINE + "auth..", RACINE + "donnees..")
            .because("une fonctionnalité doit pouvoir changer sans toucher la base commune");

    // ------------------------------------------------------------------
    // Règles transverses
    // ------------------------------------------------------------------

    /** L'API ne reçoit ni ne renvoie d'entité JPA : elle passe par des DTO. */
    @ArchTest
    static final ArchRule controleursSansEntiteEnParametre = methods()
            .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
            .and().arePublic()
            .should().notHaveRawParameterTypes(UNE_ENTITE_JPA_PARMI_LES_PARAMETRES)
            .because("une entité reçue telle quelle laisse le client fixer identifiant, dates ou stock");

    @ArchTest
    static final ArchRule controleursSansEntiteEnRetour = methods()
            .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
            .and().arePublic()
            .should().notHaveRawReturnType(ENTITES_JPA)
            .because("une entité renvoyée expose tous ses champs, présents et futurs");

    @ArchTest
    static final ArchRule entitesDansInternal = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..internal..")
            .because("une entité est un détail d'implémentation de sa fonctionnalité");

    @ArchTest
    static final ArchRule injectionParConstructeur = noFields()
            .should().beAnnotatedWith(Autowired.class)
            .because("l'injection par constructeur rend les dépendances visibles et testables sans Spring");

    @ArchTest
    static final ArchRule pasDeDateHeritee = noClasses()
            .should().dependOnClassesThat().haveFullyQualifiedName("java.util.Date")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("java.text.SimpleDateFormat")
            .because("les dates passent par java.time et sortent en ISO-8601");

    /**
     * Les packages internal et web d'une fonctionnalité ne sont utilisés que par
     * elle-même ; les autres passent par son package api.
     */
    private static ArchRule encapsulee(String fonctionnalite) {
        String paquet = RACINE + fonctionnalite;
        return classes()
                .that().resideInAnyPackage(paquet + ".internal..", paquet + ".web..")
                .should().onlyBeAccessed().byClassesThat().resideInAPackage(paquet + "..")
                .because("les autres fonctionnalités utilisent " + paquet + ".api");
    }
}
