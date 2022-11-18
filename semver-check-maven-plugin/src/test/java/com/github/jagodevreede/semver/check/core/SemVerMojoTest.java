package com.github.jagodevreede.semver.check.core;

import com.github.jagodevreede.semver.check.maven.SemVerMojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

public class SemVerMojoTest extends AbstractMojoTestCase {

    @BeforeEach
    protected void setUp() throws Exception {
        // required for mojo lookups to work
        super.setUp();
    }

    @Test
    public void testSomething() throws Exception {
        File pom = new File("target/test-classes/project-to-test/pom.xml");
        assertTrue(pom.exists());

        SemVerMojo semVerMojo = (SemVerMojo) lookupMojo("touch", pom);
        semVerMojo.setFailOnMissingFile(false);
        assertNotNull(semVerMojo);
        semVerMojo.execute();

        // TODO test something usefully
    }

}

