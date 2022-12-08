package com.github.jagodevreede.semver.check.core;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

class JarFileHelper {
    static JarFile getJarFile(final File file) throws IOException {
        return new JarFile(file, false);
    }
}
