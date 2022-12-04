package com.github.jagodevreede.semver.check.maven;


import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "touch", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class SemVerMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;
    @Parameter(defaultValue = "${project.build.finalName}", readonly = true, required = true)
    private String finalName;

    @Parameter(property = "classifier", defaultValue = "jar")
    private String classifier;

    @Parameter(property = "ignoreSnapshots", defaultValue = "true")
    private boolean ignoreSnapshots;

    @Parameter(property = "failOnMissingFile", defaultValue = "true")
    private boolean failOnMissingFile;

    @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
    private File outputDirectory;

    // Will be injected
    ArtifactMetadataSource artifactMetadataSource;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true)
    protected List<ArtifactRepository> remoteArtifactRepositories;

    @Parameter(defaultValue = "${project.pluginArtifactRepositories}", readonly = true)
    protected List<ArtifactRepository> remotePluginRepositories;

    @Parameter(defaultValue = "${localRepository}", readonly = true)
    protected ArtifactRepository localRepository;

    @Inject
    public SemVerMojo(ArtifactMetadataSource artifactMetadataSource) {
        this.artifactMetadataSource = artifactMetadataSource;
    }

    public void execute() throws MojoExecutionException {
        Log log = getLog();
        File workingFile = new File(outputDirectory, finalName + "." + classifier);
        log.debug("Using as original input file: " + workingFile);

        if (!workingFile.isFile()) {
            if (failOnMissingFile) {
                throw new MojoExecutionException("Unable to read file " + workingFile);
            }
            getLog().warn("Unable to read file " + workingFile);
            // return;
        }

        if (project == null) {
            throw new MojoExecutionException("Unable to get project information");
        }

        Artifact artifact = project.getArtifact();

        try {
            List<ArtifactVersion> artifactVersions =
                    artifactMetadataSource.retrieveAvailableVersions(artifact, localRepository, remoteArtifactRepositories)
                            .stream()
                            .filter(v -> {
                                if (ignoreSnapshots) {
                                    return !"SNAPSHOT".equals(v.getQualifier());
                                }
                                return true;
                            })
                            .sorted()
                            .collect(Collectors.toList());
            if (artifactVersions.isEmpty()) {
                log.info("No other versions available for " + artifact.getGroupId() + ":" + artifact.getArtifactId());
            }

        } catch (ArtifactMetadataRetrievalException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    public void setFailOnMissingFile(boolean failOnMissingFile) {
        this.failOnMissingFile = failOnMissingFile;
    }
}
