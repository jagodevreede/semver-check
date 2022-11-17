package com.github.jagodevreede.semver.check.core;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SemVerTypeTest {

    static Stream<Arguments> semVerTypes() {
        return Stream.of(
                Arguments.of(SemVerType.NONE, SemVerType.PATCH),
                Arguments.of(SemVerType.NONE, SemVerType.PATCH),
                Arguments.of(SemVerType.NONE, SemVerType.MINOR),
                Arguments.of(SemVerType.NONE, SemVerType.MAJOR),
                Arguments.of(SemVerType.PATCH, SemVerType.MINOR),
                Arguments.of(SemVerType.PATCH, SemVerType.MAJOR),
                Arguments.of(SemVerType.MINOR, SemVerType.MAJOR),
                Arguments.of(SemVerType.PATCH, SemVerType.PATCH),
                Arguments.of(SemVerType.MINOR, SemVerType.MINOR),
                Arguments.of(SemVerType.MAJOR, SemVerType.MAJOR)
        );
    }

    @MethodSource("semVerTypes")
    @ParameterizedTest
    void updateResult_withToBe(SemVerType was, SemVerType toBe) {
        assertThat(SemVerType.updateResult(was, toBe)).isEqualTo(toBe);
    }

    @MethodSource("semVerTypes")
    @ParameterizedTest
    void updateResult_withWas(SemVerType toBe, SemVerType was) {
        assertThat(SemVerType.updateResult(was, toBe)).isEqualTo(was);
    }
}