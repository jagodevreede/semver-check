package io.github.jagodevreede.semver.check.maven;

import io.github.jagodevreede.semver.check.core.SemVerType;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemVerMojoTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    MavenProject project;

    @Mock
    ArtifactMetadataSource artifactMetadataSource;
    @Mock
    RepositorySystem repoSystem;

    @Spy
    @InjectMocks
    SemVerMojo subject;

    @Captor
    ArgumentCaptor<String> stringCaptor;

    Artifact artifact = new DefaultArtifact("group", "id", "1.0.0", null, "jar", null, new DefaultArtifactHandler());

    @Test
    void noProjectShouldFail() {
        subject = new SemVerMojo(null, null);
        subject.haltOnFailure = true;
        assertThatThrownBy(() -> subject.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("Unable to get project information");
    }

    @Test
    void noOutputFileShouldFail() throws MojoExecutionException {
        subject.project = project;
        when(project.getArtifact()).thenReturn(artifact);
        when(project.getBuild().getDirectory()).thenReturn("");
        subject.execute();
    }

    @ParameterizedTest
    @CsvSource({
            "0.1.7, 1.0.0, 1.0.0",
            "1.1.7, 1.0.0, 1.1.7",
            "1.1.7, 1.1.7, 1.1.7",
    })
    void compareWithParentShouldGiveCorrectResult(String currentProjectVersion, String versionOfParent, String excepted) throws MojoExecutionException {
        when(project.getBuild().getDirectory()).thenReturn(new File("target/").getAbsolutePath());
        subject.outputFileName = "parent";
        subject.writeFile(project, versionOfParent);

        doNothing().when(subject).writeFile(any(), stringCaptor.capture());

        subject.updateParentOutputFile(currentProjectVersion, project);

        assertThat(stringCaptor.getValue()).isEqualTo(excepted);
    }

    @Test
    void writeFileShouldNotOverwriteIfOverwriteFlagIsDisabled() throws MojoExecutionException {
        File outputFolder = new File("target/");
        subject.outputFileName = "testVersionFile.txt";
        File outputFile = new File(outputFolder, subject.outputFileName);
        when(project.getBuild().getDirectory()).thenReturn(outputFolder.getAbsolutePath());

        subject.overwriteOutputFile = false;
        subject.writeFile(project, "0.0.1");

        assertThat(outputFile).exists();

        subject.writeFile(project, "0.0.2");

        assertThat(outputFile).hasContent("0.0.1");
    }

    @ParameterizedTest
    @CsvSource({
            "1.2.3, MAJOR, 2.0.0",
            "1.2.3, MINOR, 1.3.0",
            "1.2.3, PATCH, 1.2.4",
            "1.2.3, NONE, 1.2.3",
    })
    void testGetNextVersion(String version, SemVerType semVerType, String expected) {
        String actual = subject.getNextVersion(version, semVerType);
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "1.2.3, 2.0.0, MAJOR",
            "1.2.3, 1.3.0, MINOR",
            "1.2.3, 1.2.4, PATCH",
            "1.2.3, 1.2.3, NONE",
            // current version is lower than released version
            "1.2.3, 1.2.2, NONE",
            "1.5.3, 1.2.2, NONE",
            "2.5.3, 1.2.2, NONE",
    })
    void getCurrentSemVerType(String oldVersion, String currentVersion, SemVerType expected) {
        ArtifactVersion currentVersionArtifactVersion = new DefaultArtifactVersion(currentVersion);
        SemVerType actual = subject.getCurrentSemVerType(oldVersion, currentVersionArtifactVersion);
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "MAJOR, PATCH, false",
            "MAJOR, MINOR, false",
            "PATCH, NONE, false",
            "NONE, MAJOR, false",
            "NONE, PATCH, false",
    })
    void failOnIncorrectVersion_shouldBreakTheBuild(SemVerType detected, SemVerType current, boolean allowHigherVersions) {
        subject.failOnIncorrectVersion = true;
        subject.allowHigherVersions = allowHigherVersions;
        assertThatThrownBy(() -> {
            subject.failOnIncorrectVersion(detected, current);
        }).isInstanceOf(MojoExecutionException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "MAJOR, MAJOR, false",
            "MINOR, MINOR, false",
            "PATCH, PATCH, false",
            "MAJOR, MAJOR, true",
            "PATCH, MINOR, true",
            "PATCH, MAJOR, true",
            "MINOR, MAJOR, true",
            "NONE, MAJOR, true",
            "NONE, PATCH, true",
    })
    void failOnIncorrectVersion_shouldNotBreakTheBuildIfCorrect(SemVerType detected, SemVerType current, boolean allowHigherVersions) throws MojoExecutionException {
        subject.failOnIncorrectVersion = true;
        subject.allowHigherVersions = allowHigherVersions;
        subject.failOnIncorrectVersion(detected, current);
    }

}

