package io.github.jagodevreede.semver.check.maven;

import java.io.File;
import java.util.List;
import java.util.Objects;

import io.github.jagodevreede.semver.check.core.SemVerType;
import org.apache.maven.model.Dependency;

public class VersionInfo {
    private final String groupId;
    private final String artifactId;
    private String nextVersion;
    private SemVerType semVerType;
    private final String version;
    private final String packaging;
    private final File pomFile;
    private final List<Dependency> dependencies;

    public VersionInfo(final String groupId, final String artifactId, final String nextVersion, final SemVerType semVerType, final String version, final String packaging,
                       final File pomFile,
                       final List<Dependency> dependencies) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.nextVersion = nextVersion;
        this.semVerType = semVerType;
        this.version = version;
        this.packaging = packaging;
        this.pomFile = pomFile;
        this.dependencies = dependencies;
    }

    public String getNextVersion() {
        return nextVersion;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public SemVerType getSemVerType() {
        return semVerType;
    }

    public String getVersion() {
        return version;
    }

    public String getPackaging() {
        return packaging;
    }

    public File getPomFile() {
        return pomFile;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void setNextVersion(final String nextVersion) {
        this.nextVersion = nextVersion;
    }

    public void setSemVerType(final SemVerType semVerType) {
        this.semVerType = semVerType;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof VersionInfo)) {
            return false;
        }
        final VersionInfo that = (VersionInfo) o;
        return Objects.equals(groupId, that.groupId) && Objects.equals(artifactId, that.artifactId) && Objects.equals(version, that.version)
                && Objects.equals(packaging, that.packaging);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version, packaging);
    }

    @Override
    public String toString() {
        return "VersionInfo{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", packaging='" + packaging + '\'' +
                '}';
    }

}
