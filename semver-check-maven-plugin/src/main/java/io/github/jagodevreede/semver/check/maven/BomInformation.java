package io.github.jagodevreede.semver.check.maven;

public class BomInformation {
    private final String groupId;
    private final String artifactId;
    private final String fileLocation;

    public BomInformation(final String groupId, final String artifactId, final String fileLocation) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.fileLocation = fileLocation;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getFileLocation() {
        return fileLocation;
    }

    @Override
    public String toString() {
        return "BomInformation{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", fileLocation='" + fileLocation + '\'' +
                '}';
    }
}
