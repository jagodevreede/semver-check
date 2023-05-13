package io.github.jagodevreede.semver.check.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Inspiration from <a href="http://wush.net/svn/mindprod/com/mindprod/jarcheck/JarCheck.java">JarCheck</a>
 */
public class ClassVersion {
    private static final Logger log = LoggerFactory.getLogger(ClassVersion.class);

    private static final int CHUNK_LENGTH = 8;

    /**
     * expected first 4 bytes of a class file
     */
    private static final byte[] EXPECTED_MAGIC_NUMBER = {(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe};

    public static int getVersionNumber(JarFile jarFile) {
        final AtomicInteger maxVersion = new AtomicInteger(0);
        jarFile.entries().asIterator().forEachRemaining(jarEntry -> {
            int version = getVersionNumberFromEntry(jarFile, jarEntry);
            if (version > maxVersion.get()) {
                maxVersion.set(version);
            }
        });
        return maxVersion.get();
    }

    private static int getVersionNumberFromEntry(JarFile jarFile, JarEntry entry) {
        String elementName = entry.getName();
        if (!elementName.endsWith(".class")) {
            // ignore anything but a .final class file
            return 0;
        }
        byte[] chunk = new byte[CHUNK_LENGTH];
        try (InputStream stream = jarFile.getInputStream(entry)) {
            int bytesRead = stream.read(chunk, 0, CHUNK_LENGTH);
            if (bytesRead != CHUNK_LENGTH) {
                log.warn("Unable to read class file {}.", elementName);
                return 0;
            }
            // make sure magic number signature is as expected.
            for (int i = 0; i < EXPECTED_MAGIC_NUMBER.length; i++) {
                if (chunk[i] != EXPECTED_MAGIC_NUMBER[i]) {
                    log.warn("Magic number signature not as expected for {}.", elementName);
                    return 0;
                }
            }
            /*
             * pick out big-endian ushort major version in last two bytes of chunk
             */
            return ((chunk[CHUNK_LENGTH - 2] & 0xff) << 8) + (chunk[CHUNK_LENGTH - 1] & 0xff);
        } catch (IOException e) {
            log.warn("Problem reading class file {}.", elementName);
            return 0;
        }
    }
}
