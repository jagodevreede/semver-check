package io.github.jagodevreede.semver.check.maven;

import io.github.jagodevreede.semver.check.core.SemVerChecker;
import io.github.jagodevreede.semver.check.core.SemVerType;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class SemVerMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;
    @Parameter(defaultValue = "${project.build.finalName}", readonly = true, required = true)
    String finalName;

    @Parameter(property = "ignoreSnapshots", defaultValue = "true")
    boolean ignoreSnapshots;

    @Parameter(property = "failOnMissingFile", defaultValue = "true")
    boolean failOnMissingFile;

    @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
    File outputDirectory;

    @Parameter(property = "outputToFile")
    File outputToFile;

    // Yes use the deprecated class as the new version is not injected?
    ArtifactMetadataSource artifactMetadataSource;

    final RepositorySystem repoSystem;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true)
    List<ArtifactRepository> remoteArtifactRepositories;

    @Parameter(defaultValue = "${localRepository}", readonly = true)
    ArtifactRepository localRepository;

    @Inject
    public SemVerMojo(ArtifactMetadataSource artifactMetadataSource, RepositorySystem repoSystem) {
        this.artifactMetadataSource = artifactMetadataSource;
        this.repoSystem = repoSystem;
    }

    public void execute() throws MojoExecutionException {
        if (project == null) {
            throw new MojoExecutionException("Unable to get project information");
        }
        Artifact artifact = project.getArtifact();
        File workingFile = new File(outputDirectory, finalName + "." + (artifact.getClassifier() != null ? artifact.getClassifier() : "jar"));
        getLog().debug("Using as original input file: " + workingFile);

        if (!workingFile.isFile()) {
            if (failOnMissingFile) {
                throw new MojoExecutionException("Unable to read file " + workingFile);
            }
            getLog().warn("Unable to read file " + workingFile);
            return;
        }

        try {
            determineVersionInformation(artifact, workingFile);
        } catch (ArtifactMetadataRetrievalException | IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    private void determineVersionInformation(Artifact artifact, File workingFile) throws ArtifactMetadataRetrievalException, IOException, MojoExecutionException {
        List<ArtifactVersion> artifactVersions = getArtifactVersions(artifact);
        if (artifactVersions.isEmpty()) {
            getLog().info("No other versions available for " + artifact.getGroupId() + ":" + artifact.getArtifactId());
        } else {
            ArtifactVersion artifactVersion = artifactVersions.get(0);
            File file = getLastVersion(artifact, artifactVersion);
            if (!file.exists()) {
                getLog().warn("Artifact " + artifactVersion + " has no attached file?");
            } else {
                getLog().info("Checking SemVer against last known version " + artifactVersion);
                SemVerChecker semVerChecker = new SemVerChecker(workingFile, file);
                SemVerType semVerType = semVerChecker.determineSemVerType();
                String nextVersion = getNextVersion(artifactVersion, semVerType);
                getLog().info("Determined SemVer type as: " +
                        semVerType.toString().toLowerCase(Locale.ROOT) +
                        ", next version should be: " + nextVersion);
                if (outputToFile != null) {
                    try (FileWriter fileWriter = new FileWriter(outputToFile, UTF_8)) {
                        fileWriter.append(nextVersion);
                    }
                }
            }
        }
    }

    private File getLastVersion(Artifact artifact, ArtifactVersion artifactVersion) throws MojoExecutionException {
        getLog().debug("Using version " + artifactVersion.toString() + " to resolve");

        Artifact aetherArtifact = new DefaultArtifact(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifactVersion.toString(),
                "provided",
                artifact.getType(),
                artifact.getClassifier(),
                artifact.getArtifactHandler());

        ArtifactResolutionResult resolutionResult = repoSystem.resolve(new ArtifactResolutionRequest()
                .setLocalRepository(localRepository)
                .setRemoteRepositories(this.remoteArtifactRepositories)
                .setResolveTransitively(false)
                .setResolveRoot(true)
                .setArtifact(aetherArtifact));
        if (!resolutionResult.isSuccess()) {
            throw new MojoExecutionException("Unable to to resolve");
        }
        var result = resolutionResult.getArtifacts().stream()
                .map(Artifact::getFile)
                .toArray(File[]::new);
        return result[0];
    }

    private String getNextVersion(ArtifactVersion artifactVersion, SemVerType semVerType) {
        String nextVersion = "?";
        switch (semVerType) {
            case MAJOR:
                nextVersion = artifactVersion.getMajorVersion() + 1 + ".0.0";
                break;
            case MINOR:
                nextVersion = artifactVersion.getMajorVersion() + "." + artifactVersion.getMinorVersion() + 1 + ".0";
                break;
            case PATCH:
                nextVersion = artifactVersion.getMajorVersion() + "." + artifactVersion.getMinorVersion() + "." + artifactVersion.getIncrementalVersion() + 1;
                break;
            case NONE:
                nextVersion = artifactVersion.getMajorVersion() + "." + artifactVersion.getMinorVersion() + "." + artifactVersion.getIncrementalVersion();
                break;
        }
        return nextVersion;
    }

    private List<ArtifactVersion> getArtifactVersions(Artifact artifact) throws ArtifactMetadataRetrievalException {
        return artifactMetadataSource.retrieveAvailableVersions(artifact, localRepository, remoteArtifactRepositories)
                .stream()
                .filter(v -> {
                    if (ignoreSnapshots) {
                        return !"SNAPSHOT".equals(v.getQualifier());
                    }
                    return true;
                })
                .sorted()
                .collect(Collectors.toList());
    }

    // Visible for testing
    public void setFailOnMissingFile(boolean failOnMissingFile) {
        this.failOnMissingFile = failOnMissingFile;
    }
}
