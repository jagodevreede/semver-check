package io.github.jagodevreede.semver.check.core;

import java.util.List;

public class Configuration {

    private final List<String> excludePackages;
    private final List<String> excludeFiles;

    public Configuration(List<String> excludePackages, List<String> excludeFiles) {
        this.excludePackages = excludePackages;
        this.excludeFiles = excludeFiles;
    }

    public List<String> getExcludePackages() {
        return excludePackages;
    }

    public List<String> getExcludeFiles() {
        return excludeFiles;
    }
}
