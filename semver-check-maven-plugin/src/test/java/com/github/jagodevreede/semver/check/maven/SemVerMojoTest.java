package com.github.jagodevreede.semver.check.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemVerMojoTest {

    @Mock
    MavenProject project;

    @Mock
    ArtifactMetadataSource artifactMetadataSource;
    @Mock
    RepositorySystem repoSystem;

    @InjectMocks
    SemVerMojo subject;

    Artifact artifact = new DefaultArtifact("group", "id", "1.0.0", null, "jar", null, new DefaultArtifactHandler());

    @Test
    void noProjectShouldFail() {
        subject = new SemVerMojo(null, null);
        assertThatThrownBy(() -> subject.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("Unable to get project information");
    }

    @Test
    void noOutputFileShouldFail() throws MojoExecutionException {
        subject.project = project;
        when(project.getArtifact()).thenReturn(artifact);
        subject.execute();
    }

}

