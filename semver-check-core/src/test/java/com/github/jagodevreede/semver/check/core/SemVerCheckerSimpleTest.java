package com.github.jagodevreede.semver.check.core;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class SemVerCheckerSimpleTest {

    private final File baseJar;
    private final File additionJar;

    SemVerCheckerSimpleTest() {
        baseJar = new File("../sample/sample-base/target/semver-check-sample-base-1.0.0-SNAPSHOT.jar");
        additionJar = new File("../sample/sample-addition/target/semver-check-sample-addition-1.1.0-SNAPSHOT.jar");
    }

    @Test
    void determineSemVerType_sameJarShouldResultInNoChange() throws Exception {
        final SemVerChecker subject = new SemVerChecker(baseJar, baseJar);
        var result = subject.determineSemVerType();

        assertThat(result).isEqualTo(SemVerType.NONE);
    }

    @Test
    void determineSemVerType_additionIsMinor() throws Exception {
        final SemVerChecker subject = new SemVerChecker(baseJar, additionJar);
        var result = subject.determineSemVerType();

        assertThat(result).isEqualTo(SemVerType.MINOR);
    }

    @Test
    void determineSemVerType_removalOfMethodIsMajor() throws Exception {
        final SemVerChecker subject = new SemVerChecker(additionJar, baseJar);
        var result = subject.determineSemVerType();

        assertThat(result).isEqualTo(SemVerType.MAJOR);
    }
}