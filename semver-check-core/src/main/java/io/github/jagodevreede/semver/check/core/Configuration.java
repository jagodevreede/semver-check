package io.github.jagodevreede.semver.check.core;

import java.util.List;

public class Configuration {

    private final List<String> excludePackages;
    private final List<String> excludeFiles;
    private final List<String> runtimeClasspathElements;

    public Configuration(List<String> excludePackages, List<String> excludeFiles, List<String> runtimeClasspathElements) {
        this.excludePackages = excludePackages;
        this.excludeFiles = excludeFiles;
        this.runtimeClasspathElements = runtimeClasspathElements;
    }

    public List<String> getExcludePackages() {
        return excludePackages;
    }

    public List<String> getExcludeFiles() {
        return excludeFiles;
    }

    public List<String> getRuntimeClasspathElements() {
        return runtimeClasspathElements;
    }
}
