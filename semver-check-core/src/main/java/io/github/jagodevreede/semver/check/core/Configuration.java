package io.github.jagodevreede.semver.check.core;

import java.util.List;

public class Configuration {

    private final List<String> excludePackages;

    public Configuration(List<String> excludePackages) {
        this.excludePackages = excludePackages;
    }

    public List<String> getExcludePackages() {
        return excludePackages;
    }
}
