package com.github.jagodevreede.semver.check.maven;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "touch", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class SemVerMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project.build.finalName}", readonly = true)
    private String finalName;

    @Parameter(property = "classifier", defaultValue = "jar")
    private String classifier;

    @Parameter(property = "failOnMissingFile", defaultValue = "true")
    private boolean failOnMissingFile;

    @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
    private File outputDirectory;

    public void execute() throws MojoExecutionException {
        Log log = getLog();
        File workingFile = new File(outputDirectory, finalName + "." + classifier);
        log.debug("Using as original input file: " + workingFile);

        if (!workingFile.isFile() && failOnMissingFile) {
            throw new MojoExecutionException("Unable to read file " + workingFile);
        }

    }

    public void setFailOnMissingFile(boolean failOnMissingFile) {
        this.failOnMissingFile = failOnMissingFile;
    }
}
