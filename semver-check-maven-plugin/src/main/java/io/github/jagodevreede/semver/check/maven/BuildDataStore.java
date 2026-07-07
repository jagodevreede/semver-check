package io.github.jagodevreede.semver.check.maven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.codehaus.plexus.component.annotations.Component;

@Component(role = BuildDataStore.class)
public class BuildDataStore {
    private final AtomicReference<BomInformation> bomInformation = new AtomicReference<>();
    private final Map<String, VersionInfo> moduleData = new ConcurrentHashMap<>();

    public void store(VersionInfo data) {
        moduleData.put(data.getGroupId() + ":" + data.getArtifactId(), data);
    }

    public VersionInfo getStored(String groupAndArtifactId) {
        return moduleData.get(groupAndArtifactId);
    }

    public BomInformation getBomInformation() {
        return bomInformation.get();
    }

    public void setBomInformation(final BomInformation bomInformation) {
        this.bomInformation.updateAndGet(existing -> {
            if (existing != null && (existing.getGroupId() != null || existing.getArtifactId() != null)) {
                return existing;
            }
            return bomInformation;
        });
    }

    public Map<String, VersionInfo> getAll() {
        return Collections.unmodifiableMap(moduleData);
    }

    public List<VersionInfo> getValues() {
        return new ArrayList<>(moduleData.values());
    }
}
