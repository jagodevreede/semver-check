package io.github.jagodevreede.semver.check.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SemVerCheckerSimpleTest {

    private static final List<String> DEFAULT_EXCLUDED_FILES = List.of("META-INF/maven/");
    private final File baseJar;
    private final File additionJar;
    private final File changedJavaVersionJar;
    private final Configuration emptyConfiguration = new Configuration(List.of(), List.of(), DEFAULT_EXCLUDED_FILES, List.of());

    SemVerCheckerSimpleTest() {
        baseJar = new File("../sample/sample-base/target/semver-check-sample-base-1.0.0-SNAPSHOT.jar");
        additionJar = new File("../sample/sample-addition/target/semver-check-sample-addition-1.1.0-SNAPSHOT.jar");
        changedJavaVersionJar = new File("../sample/sample-java-version/target/semver-check-sample-java-version-1.0.0-SNAPSHOT.jar");
    }

    @Test
    void determineSemVerType_sameJarShouldResultInNoChange() throws Exception {
        final SemVerChecker subject = new SemVerChecker(baseJar, baseJar, emptyConfiguration);
        var result = subject.determineSemVerType();

        assertThat(result).isEqualTo(SemVerType.NONE);
    }

    @Test
    void determineSemVerType_additionIsMinor() throws Exception {
        final SemVerChecker subject = new SemVerChecker(baseJar, additionJar, emptyConfiguration);
        var result = subject.determineSemVerType();

        assertThat(result).isEqualTo(SemVerType.MINOR);
    }

    @Test
    void determineServerType_additionWithNewDependencyIsMinor() throws Exception {
        String userHome = System.getProperty("user.home");
        List<String> dependencyLocations = List.of("../sample/sample-dependency/target/classes", userHome + "/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar");
        File dependencyAddedJar = new File("../sample/sample-dependency/target/semver-check-sample-dependency-1.0.0-SNAPSHOT.jar");
        Configuration configuration = new Configuration(List.of(), List.of(), DEFAULT_EXCLUDED_FILES, dependencyLocations);
        final SemVerChecker subject = new SemVerChecker(baseJar, dependencyAddedJar, configuration);
        var result = subject.determineSemVerType();

        assertThat(result).isEqualTo(SemVerType.MINOR);
    }

    @CsvSource({
            "io.github.jagodevreede.semver.sample",
            "io.github.jagodevreede.*",
            "io.*"
    })
    @ParameterizedTest
    void determineSemVerType_additionIsNoneIfPackageExcluded(String excludedPackage) throws Exception {
        Configuration configuration = new Configuration(List.of(), List.of("com.acme", excludedPackage), DEFAULT_EXCLUDED_FILES, List.of());
        final SemVerChecker subject = new SemVerChecker(baseJar, additionJar, configuration);
        var result = subject.determineSemVerType();

        assertThat(result).isEqualTo(SemVerType.NONE);
    }

    @Test
    void determineSemVerType_removalOfMethodIsMajor() throws Exception {
        final SemVerChecker subject = new SemVerChecker(additionJar, baseJar, emptyConfiguration);
        var result = subject.determineSemVerType();

        assertThat(result).isEqualTo(SemVerType.MAJOR);
    }

    @CsvSource({
            "io.github.jagodevreede.semver.sample",
            "io.github.jagodevreede.*",
            "io.*"
    })
    @ParameterizedTest
    void determineSemVerType_removalOfMethodIsNoneIfPackageExcluded(String excludedPackage) throws Exception {
        Configuration configuration = new Configuration(List.of(), List.of("com.acme.*", excludedPackage), DEFAULT_EXCLUDED_FILES, List.of());
        final SemVerChecker subject = new SemVerChecker(additionJar, baseJar, configuration);
        var result = subject.determineSemVerType();

        assertThat(result).isEqualTo(SemVerType.NONE);
    }

    @Test
    void determineSemVerType_changingJavaVersionIsMajorIfHigher() throws Exception {
        final SemVerChecker subject = new SemVerChecker(changedJavaVersionJar, baseJar, emptyConfiguration);
        var result = subject.determineSemVerType();

        assertThat(result).isEqualTo(SemVerType.MAJOR);
    }

    @Test
    void determineSemVerType_changingJavaVersionIsMinorIfLower() throws Exception {
        final SemVerChecker subject = new SemVerChecker(baseJar, changedJavaVersionJar, emptyConfiguration);
        var result = subject.determineSemVerType();

        assertThat(result).isEqualTo(SemVerType.PATCH);
    }
}