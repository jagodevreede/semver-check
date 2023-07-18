package io.github.jagodevreede.semver.check.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SemVerCheckerSimpleTest {

    private final File baseJar;
    private final File additionJar;
    private final File changedJavaVersionJar;
    private final Configuration emptyConfiguration = new Configuration(List.of());

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

    @CsvSource({
            "io.github.jagodevreede.semver.sample",
            "io.github.jagodevreede",
            "io"
    })
    @ParameterizedTest
    void determineSemVerType_additionIsNoneIfPackageExcluded(String excludedPackage) throws Exception {
        Configuration configuration = new Configuration(List.of("com.acme", excludedPackage));
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
            "io.github.jagodevreede",
            "io"
    })
    @ParameterizedTest
    void determineSemVerType_removalOfMethodIsNoneIfPackageExcluded(String excludedPackage) throws Exception {
        Configuration configuration = new Configuration(List.of("com.acme", excludedPackage));
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