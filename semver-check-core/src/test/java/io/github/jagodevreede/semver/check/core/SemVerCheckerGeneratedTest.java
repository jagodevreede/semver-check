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
    private final Configuration emptyConfiguration = new Configuration(List.of(), List.of(), List.of(), List.of(), MINOR, MAJOR);

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

    @Test
    void addedNonPublicClass() throws Exception {
        var gen = new TestDataGenerator("NonPublicClass", false);
        gen.compileClass();
        gen.addClassToJar(jarAsChanged);

        checkAndReversed(PATCH, PATCH);
    }

    @Test
    void addedPublicClass() throws Exception {
        var gen = new TestDataGenerator("PublicClass", true);
        gen.compileClass();
        gen.addClassToJar(jarAsChanged);

        checkAndReversed(MINOR, MAJOR);
    }

    @Test
    void changedParameterName() throws Exception {
        var gen = new TestDataGenerator("ClassB");
        gen.add("public void somethingYouShouldSee(String a) {}");
        gen.compileClass();
        gen.addClassToJar(jarAsOriginal);

        gen = new TestDataGenerator("ClassB");
        gen.add("public void somethingYouShouldSee(String b) {}");
        gen.compileClass();
        gen.addClassToJar(jarAsChanged);

        check(NONE);
        checkReversed(NONE);
    }

    @Test
    void changedReturnType() throws Exception {
        var gen = new TestDataGenerator("ClassB");
        gen.add("public void somethingYouShouldSee() {}");
        gen.compileClass();
        gen.addClassToJar(jarAsOriginal);

        gen = new TestDataGenerator("ClassB");
        gen.add("public boolean somethingYouShouldSee() { return false; }");
        gen.compileClass();
        gen.addClassToJar(jarAsChanged);

        check(MINOR);
        checkReversed(MAJOR);
    }

    @Test
    void changedThrows() throws Exception {
        var gen = new TestDataGenerator("ClassB");
        gen.add("public void somethingYouShouldSee(String a) throws Exception {}");
        gen.compileClass();
        gen.addClassToJar(jarAsOriginal);

        gen = new TestDataGenerator("ClassB");
        gen.add("public void somethingYouShouldSee(String b) throws java.io.IOException {}");
        gen.compileClass();
        gen.addClassToJar(jarAsChanged);

        check(MAJOR);
        checkReversed(MAJOR);
    }

    @Test
    void removedThrows() throws Exception {
        var gen = new TestDataGenerator("ClassB");
        gen.add("public void somethingYouShouldSee(String a) {}");
        gen.compileClass();
        gen.addClassToJar(jarAsOriginal);

        gen = new TestDataGenerator("ClassB");
        gen.add("public void somethingYouShouldSee(String a) throws java.io.IOException {}");
        gen.compileClass();
        gen.addClassToJar(jarAsChanged);

        check(MAJOR);
        checkReversed(MAJOR);
    }

    @Test
    void changedMultipleParameters() throws Exception {
        var gen = new TestDataGenerator("ClassB");
        gen.add("public void somethingYouShouldSee(String a, String b) {}");
        gen.compileClass();
        gen.addClassToJar(jarAsOriginal);

        gen = new TestDataGenerator("ClassB");
        gen.add("public void somethingYouShouldSee(String a, int b) {}");
        gen.compileClass();
        gen.addClassToJar(jarAsChanged);

        check(MAJOR);
        checkReversed(MAJOR);
    }

    @Test
    void changedMultipleParameters_swap() throws Exception {
        var gen = new TestDataGenerator("ClassB");
        gen.add("public void somethingYouShouldSee(int b, String a) {}");
        gen.compileClass();
        gen.addClassToJar(jarAsOriginal);

        gen = new TestDataGenerator("ClassB");
        gen.add("public void somethingYouShouldSee(String a, int b) {}");
        gen.compileClass();
        gen.addClassToJar(jarAsChanged);

        check(MAJOR);
        checkReversed(MAJOR);
    }

    @Test
    void changedAnnotation() throws Exception {
        var gen = new TestDataGenerator("ClassB");
        gen.add("@Deprecated(since = \"1\")");
        gen.add("public void somethingYouShouldSee() {}");
        gen.compileClass();
        gen.addClassToJar(jarAsOriginal);

        gen = new TestDataGenerator("ClassB");
        gen.add("@Deprecated(since = \"2\")");
        gen.add("public void somethingYouShouldSee() {}");
        gen.compileClass();
        gen.addClassToJar(jarAsChanged);

        checkReversed(PATCH);
    }

    @Test
    void changedMethodSignatureReturnTypeGenericListType() throws Exception {
        var gen = new TestDataGenerator("ClassB", true, "java.util.List");
        gen.add("public List<Integer> somethingYouShouldSee() { return List.of(); }");
        gen.compileClass();
        gen.addClassToJar(jarAsOriginal);

        gen = new TestDataGenerator("ClassB", true, "java.util.List");
        gen.add("public List<Long> somethingYouShouldSee() { return List.of(); }");
        gen.compileClass();
        gen.addClassToJar(jarAsChanged);

        check(MAJOR);
        checkReversed(MAJOR);
    }

    @Nested
    class classAInOriginalWithPublicApi {
        @BeforeEach
        void createScenarioForOriginalJar() throws IOException {
            var gen = new TestDataGenerator("ClassA");
            gen.add("public void somethingYouShouldSee() {}");
            gen.compileClass();
            gen.addClassToJar(jarAsOriginal);
        }

        @Test
        void changedMethodSignatureAddedThrows () throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("public void somethingYouShouldSee() throws Exception {}");
            gen.compileClass();
            gen.addClassToJar(jarAsChanged);

            check(MAJOR);
            checkReversed(MAJOR);
        }

        @Test
        void changedMethodSignatureReturnType() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("public int somethingYouShouldSee() { return 1; }");
            gen.compileClass();
            gen.addClassToJar(jarAsChanged);

            check(MINOR);
            checkReversed(MAJOR);
        }

        @Test
        void protectedMethodToPublic() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("protected void somethingYouShouldSee() {}");
            gen.compileClass();
            gen.addClassToJar(jarAsChanged);

            checkReversed(MINOR);
        }

        @Test
        void addedAnnotationToMethod() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("@Deprecated");
            gen.add("public void somethingYouShouldSee() {}");
            gen.compileClass();
            gen.addClassToJar(jarAsChanged);

            check(MINOR);
        }

        @Test
        void addedAnnotationToMethodWithConfigOverride() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("@Deprecated");
            gen.add("public void somethingYouShouldSee() {}");
            gen.compileClass();
            gen.addClassToJar(jarAsChanged);

            Configuration configuration = new Configuration(List.of(), List.of(".*example$"), List.of(), List.of(), PATCH, MAJOR);
            check(PATCH, configuration);
        }

        @Test
        void removedAnnotationFromMethod() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("@Deprecated");
            gen.add("public void somethingYouShouldSee() {}");
            gen.compileClass();
            gen.addClassToJar(jarAsChanged);

            checkReversed(MAJOR);
        }

        @Test
        void removedAnnotationFromMethodWithConfigOverride() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("@Deprecated");
            gen.add("public void somethingYouShouldSee() {}");
            gen.compileClass();
            gen.addClassToJar(jarAsChanged);

            Configuration configuration = new Configuration(List.of(), List.of(".*example$"), List.of(), List.of(), MINOR, PATCH);
            checkReversed(PATCH, configuration);
        }
    }

    @Nested
    class emptyClassAInOriginal {
        @BeforeEach
        void createScenarioOriginalJar() throws IOException {
            var gen = new TestDataGenerator("ClassA");
            gen.add("private void somethingYouShouldNotSee() {}");
            gen.compileClass();
            gen.addClassToJar(jarAsOriginal);
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
            gen.addClassToJar(jarAsChanged);

            check(PATCH);
        }

        @Test
        void addedPublicMethod() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("public void somethingPublic() {}");
            gen.compileClass();
            gen.addClassToJar(jarAsChanged);

            check(MINOR);
        }

        @Test
        void addedPublicStaticField() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("public static final String SOMETHING = \"something\";");
            gen.compileClass();
            gen.addClassToJar(jarAsChanged);

            check(MINOR);
        }

        @Test
        void addedConstructorWithParam() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("public ClassA(int i) {}");
            gen.compileClass();
            gen.addClassToJar(jarAsChanged);

            check(MAJOR);
        }

        @Test
        void addedConstructorWithParamAndDefault() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("public ClassA() {}");
            gen.add("public ClassA(int i) {}");
            gen.compileClass();
            gen.addClassToJar(jarAsChanged);

            checkAndReversed(MINOR, MAJOR);
        }

        @Test
        void addedManualConstructor() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("public ClassA() {super();}");
            gen.compileClass();
            gen.addClassToJar(jarAsChanged);

            checkAndReversed(PATCH, PATCH); // byte code is different
        }

        @Test
        void addedManualConstructorWithAnnotation() throws Exception {
            var gen = new TestDataGenerator("ClassA");
            gen.add("@Deprecated");
            gen.add("public ClassA() {super();}");
            gen.compileClass();
            gen.addClassToJar(jarAsChanged);

            checkAndReversed(MINOR, MAJOR); // byte code is different
        }
    }

    @Nested
    class packageAdded {
        @BeforeEach
        void createScenario() throws IOException {
            var gen = new TestDataGenerator("com.example", "ClassInPackage");
            gen.compileClass();
            gen.addClassToJar(jarAsChanged);
        }

        @Test
        void addedNewPackageAndClass() throws Exception {
            checkAndReversed(MINOR, MAJOR);
        }

        @Test
        void onlyIncludeNormalPackage() throws Exception {
            Configuration configuration = new Configuration(List.of("io.github.jagodevreede.semver.sample"), List.of(), List.of(), List.of(), MINOR, MAJOR);
            check(NONE, configuration);
        }

        @Test
        void onlyIncludeNormalPackageRegex() throws Exception {
            Configuration configuration = new Configuration(List.of(".*api$", ".*sample$", "otherthing.*"), List.of(), List.of(), List.of(), MINOR, MAJOR);
            check(NONE, configuration);
        }

        @Test
        void excludeNewPackage() throws Exception {
            Configuration configuration = new Configuration(List.of(), List.of("com.example"), List.of(), List.of(), MINOR, MAJOR);
            check(NONE, configuration);
        }

        @Test
        void excludeNewPackageRegex() throws Exception {
            Configuration configuration = new Configuration(List.of(), List.of(".*example$"), List.of(), List.of(), MINOR, MAJOR);
            check(NONE, configuration);
        }
    }

    @Nested
    class resourceFile {
        @Test
        void addedFile() throws Exception {
            var gen = new TestDataGenerator("resource");
            gen.add("Some text");
            gen.writeFile(".txt");
            gen.addFileToJar(jarAsChanged, ".txt", "/");

            checkAndReversed(PATCH, MAJOR);
        }

        @Test
        void changeFile() throws Exception {
            var gen = new TestDataGenerator("resource");
            gen.add("Some text");
            gen.writeFile(".txt");
            gen.addFileToJar(jarAsChanged, ".txt", "/");
            gen.writeFile(".txt");
            gen.addFileToJar(jarAsOriginal, ".txt", "/");

            checkAndReversed(PATCH, PATCH);
        }
    }

    private void check(SemVerType verResult, Configuration configuration) throws IOException {
        final SemVerChecker subject = new SemVerChecker(jarAsOriginal, jarAsChanged, configuration);
        var result = subject.determineSemVerType();
        assertThat(result).as("result").isEqualTo(verResult);
    }

    private void check(SemVerType verResult) throws IOException {
        check(verResult, emptyConfiguration);
    }

    private void checkReversed(SemVerType verResult) throws IOException {
        checkReversed(verResult, emptyConfiguration);
    }

    private void checkReversed(SemVerType verResult, Configuration configuration) throws IOException {
        final SemVerChecker subject = new SemVerChecker(jarAsChanged, jarAsOriginal, configuration);
        var result = subject.determineSemVerType();
        assertThat(result).as("Reversed result").isEqualTo(verResult);
    }

    private void checkAndReversed(SemVerType verResult, SemVerType reversedResult) throws IOException {
        check(verResult);
        checkReversed(reversedResult);
    }

}