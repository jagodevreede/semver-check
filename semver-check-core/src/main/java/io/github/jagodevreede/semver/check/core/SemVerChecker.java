package io.github.jagodevreede.semver.check.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static io.github.jagodevreede.semver.check.core.SemVerType.*;

@SuppressWarnings("rawtypes")
public class SemVerChecker {
    private static final Logger log = LoggerFactory.getLogger(SemVerChecker.class);

    private SemVerType result = NONE;

    private final JarFile original;
    private final JarFile newJar;

    /**
     * @param original the original JAR file
     * @param newJar the new JAR file to compare against the original
     */
    public SemVerChecker(final File original, final File newJar) throws IOException {
        this(JarFileHelper.getJarFile(original), JarFileHelper.getJarFile(newJar));
    }

    /**
     * @param original the original JAR file
     * @param newJar the new JAR file to compare against the original
     */
    public SemVerChecker(final JarFile original, final JarFile newJar) {
        this.original = original;
        this.newJar = newJar;
    }

    /**
     * Determines the semantic version type (major, minor, or patch) based on the difference between two JAR files.
     *
     * The method compares the classes in the original JAR file to the classes in the new JAR file. If a class
     * in the original JAR file does not exist in the new JAR file, it is considered a major version change.
     * If a class in the original JAR file exists in the new JAR file and the two classes have differences,
     * the method calls the `determineClassDifference` method to determine the appropriate version change.
     * If a class in the original JAR file exists in the new JAR file and the two classes have no differences,
     * the method compares the byte-level content of the class in the original JAR file to the class in the new JAR file.
     * If the byte-level content is different, it is considered a patch version change. If the byte-level content is the same,
     * it is considered no change.
     *
     * @return the semantic version type
     * @throws IOException if there is an error reading from the JAR files
     */
    public SemVerType determineSemVerType() throws IOException {
        Set<Class> classesInOriginalJar = getClassesInJar(original);
        Set<Class> classesInNewJar = getClassesInJar(newJar);

        for (Class originalClass : classesInOriginalJar) {
            if (Modifier.isPublic(originalClass.getModifiers())) {
                Optional<Class> classInNewJar = classesInNewJar.stream().filter(c -> c.getName().equals(originalClass.getName())).findFirst();
                if (classInNewJar.isEmpty()) {
                    log.info("Class {} is removed", originalClass.getName());
                    result = updateResult(result, MAJOR);
                } else {
                    var classResult = determineClassDifference(originalClass, classInNewJar.get());
                    if (NONE.equals(classResult)) {
                        JarEntry originalEntry = getJarEntry(original, originalClass.getName());
                        JarEntry newEntry = getJarEntry(newJar, originalClass.getName());
                        if (originalEntry.getCrc() != newEntry.getCrc()) {
                            log.info("Class {} has been changed on byte level", originalClass.getName());
                            result = updateResult(result, PATCH);
                        } else {
                            log.debug("Class {} remains the same", originalClass.getName());
                        }
                    } else {
                        result = updateResult(result, classResult);
                    }
                }
            } else {
                log.debug("Class {} is skipped as it is not public api", originalClass.getName());
            }
        }

        return result;
    }

    private SemVerType determineClassDifference(Class originalClass, Class classInNewJar) {
        SemVerType classResult = NONE;
        Constructor[] originalClassConstructors = originalClass.getConstructors();
        Constructor[] classInNewJarConstructors = classInNewJar.getConstructors();
        SemVerType constructorResult = getSemVerType(originalClass, originalClassConstructors, classInNewJarConstructors);
        classResult = updateResult(classResult, constructorResult);

        Field[] originalClassFields = originalClass.getFields();
        Field[] inNewJarFields = classInNewJar.getFields();
        SemVerType fieldResult = getSemVerType(originalClass, originalClassFields, inNewJarFields);
        classResult = updateResult(classResult, fieldResult);

        Method[] originalClassMethods = originalClass.getMethods();
        Method[] classInNewJarMethods = classInNewJar.getMethods();
        SemVerType methodResult = getSemVerType(originalClass, originalClassMethods, classInNewJarMethods);
        classResult = updateResult(classResult, methodResult);

        return classResult;
    }

    private <M extends Member> SemVerType getSemVerType(Class originalClass, M[] originalClassMembers, M[] inNewJarMembers) {
        for (M originalClassMember : originalClassMembers) {
            M memberInNew = getMemberInOther(originalClassMember, inNewJarMembers);
            if (memberInNew == null) {
                log.info("{} '{}' no longer exists in {}", originalClassMember.getClass().getSimpleName(), originalClassMember, originalClass.getName());
                return MAJOR;
            }
        }

        for (M memberInNew : inNewJarMembers) {
            M originalClassMember = getMemberInOther(memberInNew, originalClassMembers);
            if (originalClassMember == null) {
                log.info("{} '{}' in {} is new", memberInNew.getClass().getSimpleName(), memberInNew, originalClass.getName());
                return MINOR;
            }
        }
        return NONE;
    }

    private <M extends Member> M getMemberInOther(M originalClassMethod, M[] classInNewJarMethods) {
        for (M method : classInNewJarMethods) {
            if (originalClassMethod.toString().equals(method.toString())) {
                return method;
            }
        }
        return null;
    }

    private Set<Class> getClassesInJar(JarFile jarFile) throws IOException {
        Set<String> classNames = getClassNamesFromJarFile(jarFile);
        Set<Class> classes = new HashSet<>(classNames.size());
        try (URLClassLoader cl = URLClassLoader.newInstance(
                new URL[]{new URL("jar:file:" + jarFile.getName() + "!/")})) {
            for (String name : classNames) {
                Class clazz = null; // Load the class by its name
                try {
                    clazz = cl.loadClass(name);
                } catch (ClassNotFoundException e) {
                    log.warn("Failed to load class {} from {}", name, jarFile);
                }
                classes.add(clazz);
            }
        }
        return classes;
    }

    private Set<String> getClassNamesFromJarFile(JarFile jarFile) {
        return jarFile.stream()
                .filter(jar -> jar.getName().endsWith(".class"))
                .map(jar -> jar.getName().replace("/", ".")
                        .replace(".class", ""))
                .collect(Collectors.toSet());
    }

    private JarEntry getJarEntry(JarFile jarFile, String className) {
        return jarFile.stream()
                .filter(jar -> jar.getName().endsWith(".class"))
                .filter(jar -> (jar.getName().replace("/", ".")
                        .replace(".class", "")).equals(className))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(String.format("tries to load class %s for own jar and did not find it anymore?", className)));
    }
}
