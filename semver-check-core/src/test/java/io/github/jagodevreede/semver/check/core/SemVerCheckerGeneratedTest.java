package io.github.jagodevreede.semver.check.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static io.github.jagodevreede.semver.check.core.SemVerType.*;
import static org.assertj.core.api.Assertions.assertThat;

class SemVerCheckerGeneratedTest {

    private final File baseJar;
    private final File jarAsOriginal = new File("target/original.jar");
    private final File jarAsChanged = new File("target/changed.jar");
    private final Configuration emptyConfiguration = new Configuration(List.of());

    SemVerCheckerGeneratedTest() {
        baseJar = new File("../sample/sample-base/target/semver-check-sample-base-1.0.0-SNAPSHOT.jar");
    }

    @BeforeEach
    void beforeEach() throws IOException {
        Files.copy(baseJar.toPath(), jarAsOriginal.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(baseJar.toPath(), jarAsChanged.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @AfterEach
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void AfterEach() {
        jarAsChanged.delete();
        jarAsOriginal.delete();
    }

    @Nested
    class classAInOriginalWithPublicApi {
        @BeforeEach
        void createScenarioForOriginalJar() throws IOException {
            var gen = new TestDataGenerator("ClassA");
            gen.add("public void somethingYouShouldSee() {}");
            gen.compileClass();
            gen.addToJar(jarAsOriginal);
        }

        @Test
        void addedAnnotationToMethod() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("@Deprecated");
            gen.add("public void somethingYouShouldSee() {}");
            gen.compileClass();
            gen.addToJar(jarAsChanged);

            check(MINOR);
        }

        @Test
        void removedAnnotationFromMethod() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("@Deprecated");
            gen.add("public void somethingYouShouldSee() {}");
            gen.compileClass();
            gen.addToJar(jarAsChanged);

            checkReversed(MAJOR);
        }
    }

    @Nested
    class emptyClassAInOriginal {
        @BeforeEach
        void createScenarioOriginalJar() throws IOException {
            var gen = new TestDataGenerator("ClassA");
            gen.add("private void somethingYouShouldNotSee() {}");
            gen.compileClass();
            gen.addToJar(jarAsOriginal);
        }
        
        @Test
        void classNotInChanged() throws Exception {
            check(MAJOR);
        }

        @Test
        void addedPrivateMethod() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("private void somethingElseYouShouldNotSee() {}");
            gen.compileClass();
            gen.addToJar(jarAsChanged);

            check(PATCH);
        }

        @Test
        void addedPublicMethod() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("public void somethingPublic() {}");
            gen.compileClass();
            gen.addToJar(jarAsChanged);

            check(MINOR);
        }

        @Test
        void addedPublicStaticField() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("public static final String SOMETHING = \"something\";");
            gen.compileClass();
            gen.addToJar(jarAsChanged);

            check(MINOR);
        }

        @Test
        void addedConstructorWithParam() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("public ClassA(int i) {}");
            gen.compileClass();
            gen.addToJar(jarAsChanged);

            check(MAJOR);
        }

        @Test
        void addedConstructorWithParamAndDefault() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("public ClassA() {}");
            gen.add("public ClassA(int i) {}");
            gen.compileClass();
            gen.addToJar(jarAsChanged);

            checkAndReversed(MINOR, MAJOR);
        }

        @Test
        void addedManualConstructor() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("public ClassA() {super();}");
            gen.compileClass();
            gen.addToJar(jarAsChanged);

            checkAndReversed(PATCH, PATCH); // byte code is different
        }

        @Test
        void addedManualConstructorWithAnnotation() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("@Deprecated");
            gen.add("public ClassA() {super();}");
            gen.compileClass();
            gen.addToJar(jarAsChanged);

            checkAndReversed(MINOR, MAJOR); // byte code is different
        }
    }

    private void check(SemVerType verResult) throws IOException {
        final SemVerChecker subject = new SemVerChecker(jarAsOriginal, jarAsChanged, emptyConfiguration);
        var result = subject.determineSemVerType();
        assertThat(result).as("result").isEqualTo(verResult);
    }

    private void checkReversed(SemVerType verResult) throws IOException {
        final SemVerChecker subject = new SemVerChecker(jarAsChanged, jarAsOriginal, emptyConfiguration);
        var result = subject.determineSemVerType();
        assertThat(result).as("Reversed result").isEqualTo(verResult);
    }

    private void checkAndReversed(SemVerType verResult, SemVerType reversedResult) throws IOException {
        check(verResult);
        checkReversed(reversedResult);
    }

}