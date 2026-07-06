package io.github.jagodevreede.semver.check.maven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.plexus.component.annotations.Component;

@Component(role = BuildDataStore.class)
public class BuildDataStore {
    private BomInformation bomInformation;
    private final Map<String, VersionInfo> moduleData = new ConcurrentHashMap<>();

    public void store(String artifactId, VersionInfo data) {
        moduleData.put(artifactId, data);
    }

    public BomInformation getBomInformation() {
        return bomInformation;
    }

    public void setBomInformation(final BomInformation bomInformation) {
        this.bomInformation = bomInformation;
    }

    public Map<String, VersionInfo> getAll() {
        return Collections.unmodifiableMap(moduleData);
    }

    public List<VersionInfo> getValues() {
        return new ArrayList<>(moduleData.values());
    }
}
