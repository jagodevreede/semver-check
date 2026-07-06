package io.github.jagodevreede.semver.check.maven;

import static io.github.jagodevreede.semver.check.core.SemVerType.NONE;
import static io.github.jagodevreede.semver.check.core.SemVerType.PATCH;
import static io.github.jagodevreede.semver.check.maven.MultiModuleStrategy.HIGHEST;
import static io.github.jagodevreede.semver.check.maven.MultiModuleStrategy.SEMVER;
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

    @Parameter(property = "setNoneToReleased", defaultValue = "true")
    private boolean setNoneToReleased;

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

        updateRootPomWithBom();

        for (VersionInfo versionInfo : allData.values()) {
            String nextVersion = getNextVersionWithSnapshot(versionInfo.getNextVersion(), versionInfo);

            try {
                if (SEMVER.equals(multiModuleStrategy) && NONE.equals(versionInfo.getSemVerType())) {
                    getLog().info("No version update needed for " + versionInfo.getArtifactId() + " as it has no semver change.");
                } else {
                    getLog().info("Updating version for " + versionInfo.getArtifactId() + " from " + versionInfo.getVersion() + " to " + nextVersion);
                    updatePomVersion(versionInfo, nextVersion);
                }
                updateOwnDependencies(versionInfo, allData);
            } catch (XMLStreamException | IOException e) {
                throw new MojoExecutionException("Failed to update version for " + versionInfo.getArtifactId(), e);
            }
        }

        getLog().info("Version update complete.");
    }

    private void updateRootPomWithBom() {
        VersionInfo rootVersionInfo = dataStore.getAll().get(project.getGroupId() + ":" + project.getArtifactId());
        if (rootVersionInfo == null) {
            getLog().error("Root POM version information not found in BuildDataStore for artifactId: " + project.getGroupId() + ":" + project.getArtifactId());
            return;
        }
        try {
            if (NONE.equals(rootVersionInfo.getSemVerType())) {
                rootVersionInfo.setSemVerType(PATCH);
                String nextVersion = getNextVersion(rootVersionInfo.getLastReleasedVersion(), PATCH);
                rootVersionInfo.setNextVersion(nextVersion);
                getLog().info("Root POM has no semver change, marking as patch to ensure everything can be released new version is: " + rootVersionInfo.getNextVersion());
                updatePomVersion(rootVersionInfo, rootVersionInfo.getNextVersion());
                updateOwnDependencies(rootVersionInfo, dataStore.getAll());
            }
            BomInformation bomInfo = dataStore.getBomInformation();
            if (bomInfo != null) {
                VersionInfo bomVersionInfo = dataStore.getAll().get(bomInfo.getGroupId() + ":" + bomInfo.getArtifactId());
                if (bomVersionInfo != null) {
                    if (NONE.equals(bomVersionInfo.getSemVerType())) {
                        getLog().info("BOM " + bomInfo.getArtifactId() + " has no semver change, marking as patch to ensure everything can be released.");
                        bomVersionInfo.setSemVerType(PATCH);
                        String nextVersion = getNextVersion(bomVersionInfo.getLastReleasedVersion(), PATCH);
                        bomVersionInfo.setNextVersion(nextVersion);
                    }
                    getLog().info("Updating root POM with BOM version for " + bomInfo.getArtifactId() + " to " + bomVersionInfo.getNextVersion());
                    rootVersionInfo.setNextVersion(bomVersionInfo.getNextVersion());
                    rootVersionInfo.setSemVerType(bomVersionInfo.getSemVerType());
                    updatePomVersion(bomVersionInfo, bomVersionInfo.getNextVersion());
                    updateOwnDependencies(bomVersionInfo, dataStore.getAll());
                } else {
                    getLog().warn("BOM information found but no corresponding VersionInfo in BuildDataStore for artifactId: " + bomInfo.getArtifactId());
                }

            } else {
                getLog().debug("No BOM information found in BuildDataStore.");
            }
        } catch (XMLStreamException | IOException e) {
            getLog().error("Failed to update root POM with new versions", e);
        }
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
                nextVersion = getNextVersion(maxVersionInfo.getLastReleasedVersion(), PATCH);
                maxVersionInfo.setNextVersion(nextVersion);
                getLog().info("Module " + maxVersionInfo.getArtifactId()
                        + " has been determined highest version but is not changed, marking as patch to ensure everything can be released new version is: "
                        + maxVersionInfo.getNextVersion());
            }
            versionInfo.setNextVersion(nextVersion);
        }
        if (SEMVER.equals(multiModuleStrategy)) {
            if (NONE.equals(versionInfo.getSemVerType()) && "pom".equals(versionInfo.getPackaging())) {
                boolean moduleHasDeclaredDependenciesToOtherModules = versionInfo.getDependencies().stream()
                        // We only need dependecies that are also modules of this multi module project, and have a semver change
                        .anyMatch(dep -> values.stream()
                                .anyMatch(v -> !NONE.equals(v.getSemVerType()) && v.getGroupId().equals(dep.getGroupId()) && v.getArtifactId()
                                        .equals(dep.getArtifactId())));
                if (moduleHasDeclaredDependenciesToOtherModules) {
                    // At this point the highest version is a NONE, but we have modules that have changed, need to set this to patch
                    versionInfo.setSemVerType(PATCH);
                    String nextVersion = getNextVersion(versionInfo.getLastReleasedVersion(), PATCH);
                    versionInfo.setNextVersion(nextVersion);
                    getLog().info("Module " + versionInfo.getArtifactId()
                            + " has dependencies or dependency management that as versions marked for release, so this needs to be released as well to version: "
                            + versionInfo.getNextVersion());
                }
            }
        }
    }

    private void updatePomVersion(final VersionInfo versionInfo, final String nextVersion) throws XMLStreamException, IOException {
        Path pomPath = versionInfo.getPomFile().toPath();
        try (MutableXMLStreamReader pom = new MutableXMLStreamReader(pomPath)) {
            PomHelper.setProjectVersion(pom, nextVersion);
            Files.write(pomPath, pom.getSource().getBytes());
        }
    }

    /**
     * Updates dependency versions in the current module POM when they refer to other modules in the build data store.
     *
     * @param versionInfo the module to update
     * @param allData all module version data
     *
     * @throws XMLStreamException if the POM cannot be read or written
     * @throws IOException if writing the updated POM fails
     */
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
                        String nextOtherVersion = getNextVersionWithSnapshot(otherVersion.getNextVersion(), otherVersion);
                        if (NONE.equals(otherVersion.getSemVerType()) && (SEMVER.equals(multiModuleStrategy)) && otherVersion.getLastReleasedVersion() != null) {
                            if (setNoneToReleased) {
                                getLog().info("Dependency " + dependency.getGroupId() + ":" + dependency.getArtifactId()
                                        + " has no semver change, updating to last released version: " + otherVersion.getLastReleasedVersion());
                                nextOtherVersion = otherVersion.getLastReleasedVersion();
                            } else {
                                nextOtherVersion = otherVersion.getVersion();
                            }
                        } else {
                            getLog().info(
                                    "  Dependency " + dependency.getGroupId() + ":" + dependency.getArtifactId() + " updated to " + getNextVersionWithSnapshot(
                                            otherVersion.getNextVersion(), otherVersion));
                        }
                        if (PomHelper.setDependencyVersion(
                                pom,
                                dependency.getGroupId(),
                                dependency.getArtifactId(),
                                dependency.getVersion(),
                                nextOtherVersion,
                                model,
                                getLog())) {
                            changed = true;
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
