package io.github.jagodevreede.semver.check.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Configuration {
    private static final Logger log = LoggerFactory.getLogger(Configuration.class);

    private final List<Pattern> includePackages;
    private final List<Pattern> excludePackages;
    private final List<String> excludeFiles;
    private final List<String> runtimeClasspathElements;

    public Configuration(List<String> includePackages, List<String> excludePackages, List<String> excludeFiles, List<String> runtimeClasspathElements) {
        this.includePackages = includePackages.stream().map(Pattern::compile).collect(Collectors.toList());
        this.excludePackages = excludePackages.stream().map(Pattern::compile).collect(Collectors.toList());;
        this.excludeFiles = excludeFiles;
        this.runtimeClasspathElements = runtimeClasspathElements;
    }

    public List<Pattern> getIncludePackages() {
        return includePackages;
    }

    public List<Pattern> getExcludePackages() {
        return excludePackages;
    }

    public List<String> getExcludeFiles() {
        return excludeFiles;
    }

    public List<String> getRuntimeClasspathElements() {
        return runtimeClasspathElements;
    }

    public boolean isExcluded(ClassInformation aClass) {
        for (Pattern excludePackage : getExcludePackages()) {
            if (excludePackage.matcher(aClass.getClazz().getPackage().getName()).matches()) {
                log.debug("Class {} is skipped as it is excluded from the check as it in excluded package {}", aClass.getClazz().getName(), excludePackage);
                return true;
            }
        }
        for (Pattern includePackage : getIncludePackages()) {
            if (includePackage.matcher(aClass.getClazz().getPackage().getName()).matches()) {
                return false;
            }
        }
        if (!getIncludePackages().isEmpty()) {
            log.debug("Class {} is skipped as it is not included in the check", aClass.getClazz().getName());
            return true;
        }
        return false;
    }
}
