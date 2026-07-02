package io.github.jagodevreede.semver.check.maven;

import static io.github.jagodevreede.semver.check.core.SemVerType.NONE;
import static io.github.jagodevreede.semver.check.core.SemVerType.PATCH;
import static io.github.jagodevreede.semver.check.maven.MultiModuleStrategy.HIGHEST;
import static io.github.jagodevreede.semver.check.maven.SemVerMojo.getNextVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.versions.api.PomHelper;
import org.codehaus.mojo.versions.rewriting.MutableXMLStreamReader;

@Mojo(name = "update-version", aggregator = true, threadSafe = true)
public class UpdateVersionMojo extends AbstractMojo {

    @Component
    private BuildDataStore dataStore;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Strategy to use when a multi module project is detected.
     * Possible values: HIGHEST, SEMVER
     * Default is HIGHEST.
     */
    @Parameter(property = "multiModuleStrategy", defaultValue = "HIGHEST")
    MultiModuleStrategy multiModuleStrategy;

    @Override
    public void execute() throws MojoExecutionException {
        Map<String, VersionInfo> allData = dataStore.getAll();
        if (allData.isEmpty()) {
            getLog().warn("No module data found in BuildDataStore. Did you run io.github.jagodevreede:semver-check-maven-plugin:check first?");
            return;
        }

        for (VersionInfo versionInfo : allData.values()) {
            determineRealSemVerBasedOnStrategy(allData.values(), versionInfo);
        }

        for (VersionInfo versionInfo : allData.values()) {
            String nextVersion = getNextVersionWithSnapshot(versionInfo.getNextVersion(), versionInfo);

            getLog().info("Updating version for " + versionInfo.getArtifactId() + " from " + versionInfo.getVersion() + " to " + nextVersion);

            try {
                updatePomVersion(versionInfo, nextVersion);
                updateOwnDependencies(versionInfo, allData);
            } catch (XMLStreamException | IOException e) {
                throw new MojoExecutionException("Failed to update version for " + versionInfo.getArtifactId(), e);
            }
        }

        getLog().info("Version update complete.");
    }

    private void determineRealSemVerBasedOnStrategy(final Collection<VersionInfo> values, final VersionInfo versionInfo) {
        if (HIGHEST.equals(multiModuleStrategy)) {
           VersionInfo maxVersionInfo = values.stream()
                .max(Comparator.comparing((VersionInfo v) -> new DefaultArtifactVersion(v.getNextVersion()))
                        .thenComparing(v -> NONE.equals(v.getSemVerType()) ? 0 : 1))
                .orElseThrow();
            String nextVersion = maxVersionInfo.getNextVersion();
            if (NONE.equals(maxVersionInfo.getSemVerType()) &&
                    maxVersionInfo.getDependencies().stream()
                        // We only need dependecies that are also modules of this multi-module project, and have a semver change
                        .anyMatch(dep -> values.stream()
                                .anyMatch(v -> !NONE.equals(v.getSemVerType())))) {
                // At this point the highest version is a NONE, but we have modules that have changed, need to set this to patch
                maxVersionInfo.setSemVerType(PATCH);
                nextVersion = getNextVersion(maxVersionInfo.getNextVersion(), PATCH);
                maxVersionInfo.setNextVersion(nextVersion);
                getLog().info("Module " + maxVersionInfo.getArtifactId() + " has been determined highest version but is not changed, marking as patch to ensure everything can be released new version is: " + maxVersionInfo.getNextVersion());
            }
            versionInfo.setNextVersion(nextVersion);
        }
    }

    private void updatePomVersion(final VersionInfo versionInfo, final String nextVersion) throws XMLStreamException, IOException {
        Path pomPath = versionInfo.getPomFile().toPath();
        try (MutableXMLStreamReader pom = new MutableXMLStreamReader(pomPath)) {
            PomHelper.setProjectVersion(pom, nextVersion);
            Files.write(pomPath, pom.getSource().getBytes());
        }
    }

    private void updateOwnDependencies(final VersionInfo versionInfo, final Map<String, VersionInfo> allData)
            throws XMLStreamException, IOException {
        Path pomPath = versionInfo.getPomFile().toPath();
        try (MutableXMLStreamReader pom = new MutableXMLStreamReader(pomPath)) {
            final Model model = PomHelper.getRawModel(versionInfo.getPomFile());

            List<Dependency> declaredDependencies = getDeclaredDependenciesWithVersion(model);
            if (declaredDependencies.isEmpty()) {
                return;
            }

            boolean changed = false;
            for (Dependency dependency : declaredDependencies) {
                for (VersionInfo otherVersion : allData.values()) {
                    if (versionInfo.equals(otherVersion)) {
                        continue;
                    }
                    if (dependency.getGroupId().equals(otherVersion.getGroupId())
                            && dependency.getArtifactId().equals(otherVersion.getArtifactId())) {
                        if (PomHelper.setDependencyVersion(
                                pom,
                                dependency.getGroupId(),
                                dependency.getArtifactId(),
                                dependency.getVersion(),
                                getNextVersionWithSnapshot(otherVersion.getNextVersion(), otherVersion),
                                model,
                                getLog())) {
                            changed = true;
                            getLog().info(
                                    "  Dependency " + dependency.getGroupId() + ":" + dependency.getArtifactId() + " updated to " + getNextVersionWithSnapshot(
                                            otherVersion.getNextVersion(), otherVersion));
                        }
                    }
                }
            }

            if (changed) {
                Files.write(pomPath, pom.getSource().getBytes());
            }

        }
    }

    private String getNextVersionWithSnapshot(String nextVersion, final VersionInfo versionInfo) {
        if (versionInfo.getVersion().endsWith("-SNAPSHOT") && !nextVersion.endsWith("-SNAPSHOT")) {
            return nextVersion + "-SNAPSHOT";
        }
        return nextVersion;
    }

    private List<Dependency> getDeclaredDependenciesWithVersion(final Model model) {
        List<Dependency> dependencies = new ArrayList<>();
        addDependenciesWithVersion(model.getDependencies(), dependencies);
        if (model.getDependencyManagement() != null) {
            addDependenciesWithVersion(model.getDependencyManagement().getDependencies(), dependencies);
        }

        for (Profile profile : model.getProfiles()) {
            addDependenciesWithVersion(profile.getDependencies(), dependencies);
            if (profile.getDependencyManagement() != null) {
                addDependenciesWithVersion(profile.getDependencyManagement().getDependencies(), dependencies);
            }
        }
        return dependencies;
    }

    private void addDependenciesWithVersion(final List<Dependency> source, final List<Dependency> target) {
        if (source == null) {
            return;
        }

        for (Dependency dependency : source) {
            if (dependency.getGroupId() != null && dependency.getArtifactId() != null && dependency.getVersion() != null) {
                target.add(dependency);
            }
        }
    }
}
