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
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
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
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class SemVerMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;

    @Parameter(property = "skip", defaultValue = "false")
    boolean skip;
    @Parameter(property = "ignoreSnapshots", defaultValue = "true")
    boolean ignoreSnapshots;

    @Parameter(property = "haltOnFailure", defaultValue = "true")
    boolean haltOnFailure;

    @Parameter(property = "outputFileName", defaultValue = "nextVersion.txt")
    String outputFileName;

    // Yes use the deprecated class as the new version is not injected?
    private final ArtifactMetadataSource artifactMetadataSource;

    private final RepositorySystem repoSystem;

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
        if (skip) {
            getLog().info("Skipping semantic versioning check, as skip is set to true");
            return;
        }
        try {
            haltOnCondition(project == null, "Unable to get project information");
            if ("pom".equals(project.getPackaging())) {
                getLog().info("No semantic versioning information for pom packaging");
                return;
            }
            Artifact artifact = project.getArtifact();
            File workingFile = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + "." + (artifact.getClassifier() != null ? artifact.getClassifier() : "jar"));
            getLog().debug("Using as original input file: " + workingFile);

            haltOnCondition(!workingFile.isFile(), "Unable to read file " + workingFile);

            determineVersionInformation(artifact, workingFile);
        } catch (ArtifactMetadataRetrievalException | IOException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (HaltException he) {
            getLog().warn(he.getMessage());
        }
    }

    private void determineVersionInformation(Artifact artifact, File workingFile) throws ArtifactMetadataRetrievalException, IOException, MojoExecutionException {
        List<ArtifactVersion> artifactVersions = getArtifactVersions(artifact);
        SemVerType semVerType = SemVerType.NONE;
        ArtifactVersion artifactVersion;
        if (artifactVersions.isEmpty()) {
            getLog().info("No other versions available for " + artifact.getGroupId() + ":" + artifact.getArtifactId());
            artifactVersion = new DefaultArtifactVersion(artifact.getVersion());
        } else {
            artifactVersion = artifactVersions.get(0);
            File file = getLastVersion(artifact, artifactVersion);
            if (!file.exists()) {
                getLog().warn("Artifact " + artifactVersion + " has no attached file?");
            } else {
                getLog().info("Checking SemVer against last known version " + artifactVersion);
                SemVerChecker semVerChecker = new SemVerChecker(workingFile, file);
                semVerType = semVerChecker.determineSemVerType();
            }
        }
        String nextVersion = getNextVersion(artifactVersion, semVerType);
        getLog().info("Determined SemVer type as: " +
                semVerType.toString().toLowerCase(Locale.ROOT) +
                ", next version should be: " + nextVersion);
        if (outputFileName != null) {
            writeFile(project, nextVersion);
            if (project.getParent() != null) {
                updateParent(nextVersion, project.getParent());
            }
        }
    }

    // Visible for testing
    void writeFile(MavenProject mavenProject, String nextVersion) throws MojoExecutionException {
        File outputDirectory = new File(mavenProject.getBuild().getDirectory());
        outputDirectory.mkdirs();
        try (FileWriter fileWriter = new FileWriter(new File(outputDirectory, outputFileName), UTF_8)) {
            fileWriter.append(nextVersion);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    // Visible for testing
    void updateParent(String nextVersion, MavenProject mavenProject) throws MojoExecutionException {
        String combinedNextVersion = nextVersion;
        ArtifactVersion nextArtifactVersion = new DefaultArtifactVersion(nextVersion);
        File outputFileOfProject = new File(mavenProject.getBuild().getDirectory(), outputFileName);
        if (outputFileOfProject.exists()) {
            try {
                String contentOfParent = Files.readString(outputFileOfProject.toPath());
                ArtifactVersion nextArtifactVersionOfParent = new DefaultArtifactVersion(contentOfParent);
                if (nextArtifactVersionOfParent.compareTo(nextArtifactVersion) > 0) {
                    combinedNextVersion = contentOfParent;
                }
            } catch (IOException ioe) {
                getLog().error("Unable to read " + outputFileOfProject.getAbsolutePath());
            }
        }
        writeFile(mavenProject, combinedNextVersion);
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

    /// Visible for testing
    String getNextVersion(ArtifactVersion artifactVersion, SemVerType semVerType) {
        String nextVersion = "?";
        switch (semVerType) {
            case MAJOR:
                nextVersion = (artifactVersion.getMajorVersion() + 1) + ".0.0";
                break;
            case MINOR:
                nextVersion = artifactVersion.getMajorVersion() + "." + (artifactVersion.getMinorVersion() + 1) + ".0";
                break;
            case PATCH:
                nextVersion = artifactVersion.getMajorVersion() + "." + artifactVersion.getMinorVersion() + "." + (artifactVersion.getIncrementalVersion() + 1);
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

    private void haltOnCondition(boolean condition, String message) throws MojoExecutionException, HaltException {
        if (condition) {
            if (haltOnFailure) {
                throw new MojoExecutionException(message);
            }
            throw new HaltException(message);
        }
    }
}
