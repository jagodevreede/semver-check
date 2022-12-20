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
            "1.1.7, 1.1.7, 1.1.7"
    })
    void compairWithParentShouldGiveCorrectResult(String currentProjectVersion, String versionOfParent, String excepted) throws MojoExecutionException {
        when(project.getBuild().getDirectory()).thenReturn(new File("target/").getAbsolutePath());
        subject.outputFileName = "parent";
        subject.writeFile(project, versionOfParent);

        doNothing().when(subject).writeFile(any(), stringCaptor.capture());

        subject.updateParent(currentProjectVersion, project);

        assertThat(stringCaptor.getValue()).isEqualTo(excepted);
    }

    @ParameterizedTest
    @CsvSource({
            "1.2.3, MAJOR, 2.0.0",
            "1.2.3, MINOR, 1.3.0",
            "1.2.3, PATCH, 1.2.4",
            "1.2.3, NONE, 1.2.3"
    })
    public void testGetNextVersion(String version, SemVerType semVerType, String expected) {
        ArtifactVersion artifactVersion = new DefaultArtifactVersion(version);
        String actual = subject.getNextVersion(artifactVersion, semVerType);
        assertThat(actual).isEqualTo(expected);
    }

}

