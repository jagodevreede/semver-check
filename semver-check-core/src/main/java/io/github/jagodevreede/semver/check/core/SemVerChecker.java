package io.github.jagodevreede.semver.check.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static io.github.jagodevreede.semver.check.core.SemVerType.*;

@SuppressWarnings("rawtypes")
public class SemVerChecker {
    private static final Logger log = LoggerFactory.getLogger(SemVerChecker.class);
    private static final String MODULE_INFO_CLASS_NAME = "module-info";

    private SemVerType result = NONE;

    private final JarFile original;
    private final JarFile newJar;
    private final Configuration configuration;

    /**
     * @param original the original JAR file
     * @param newJar   the new JAR file to compare against the original
     */
    public SemVerChecker(final File original, final File newJar, final Configuration configuration) throws IOException {
        this(JarFileHelper.getJarFile(original), JarFileHelper.getJarFile(newJar), configuration);
    }

    /**
     * @param original the original JAR file
     * @param newJar   the new JAR file to compare against the original
     */
    public SemVerChecker(final JarFile original, final JarFile newJar, final Configuration configuration) {
        this.original = original;
        this.newJar = newJar;
        this.configuration = configuration;
    }

    /**
     * Determines the semantic version type (major, minor, or patch) based on the difference between two JAR files.
     * <p>
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
        int originalClassVersion = getVersionOfClass(original);
        int newJarClassVersion = getVersionOfClass(newJar);

        if (originalClassVersion < newJarClassVersion) {
            log.info("The new JAR file contains a higher class version, changed from {} to {}", originalClassVersion, newJarClassVersion);
            return MAJOR;
        }

        Set<ClassInformation> classesInOriginalJar = getClassesInJar(original);
        Set<ClassInformation> classesInNewJar = getClassesInJar(newJar);

        for (ClassInformation originalClass : classesInOriginalJar) {
            if (configuration.isExcluded(originalClass)) {
                continue;
            }
            Optional<ClassInformation> classInNewJar = classesInNewJar.stream().filter(c -> c.getName().equals(originalClass.getName())).findFirst();
            var classResult = NONE;
            if (classInNewJar.isEmpty()) {
                if (Modifier.isPublic(originalClass.getClazz().getModifiers())) {
                    log.info("Public Class {} is removed", originalClass.getName());
                    classResult = MAJOR;
                } else {
                    log.info("Non-Public Class {} is removed", originalClass.getName());
                    classResult = PATCH;
                }
                result = updateResult(result, classResult);
            } else {
                if (Modifier.isPublic(originalClass.getClazz().getModifiers())) {
                    classResult = determineClassDifference(originalClass, classInNewJar.get());
                }
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
        }

        for (ClassInformation newClass : classesInNewJar) {
            if (configuration.isExcluded(newClass)) {
                continue;
            }
            Optional<ClassInformation> classInOriginalJar = classesInOriginalJar.stream().filter(c -> c.getName().equals(newClass.getName())).findFirst();
            if (classInOriginalJar.isEmpty()) {
                if (Modifier.isPublic(newClass.getClazz().getModifiers())) {
                    log.info("Public class {} has been added", newClass.getName());
                    result = updateResult(result, MINOR);
                    break;
                } else {
                    log.info("Non-public Class {} has been added", newClass.getName());
                    result = updateResult(result, PATCH);
                }

            }
        }

        if (result != MAJOR) {
            SemVerType fileDifferences = determineFileDifferences();
            result = updateResult(result, fileDifferences);
        }

        return result;
    }

    private SemVerType determineFileDifferences() throws IOException {
        Map<String, JarEntry> filesInOriginalJar = getFilesNotClassedInJar(original);
        Map<String, JarEntry> filesInNewJar = getFilesNotClassedInJar(newJar);

        for (Map.Entry<String, JarEntry> fileInOriginal : filesInOriginalJar.entrySet()) {
            if (configuration.isFileExcluded(fileInOriginal.getKey())) {
                continue;
            }
            JarEntry fileInNewJar = filesInNewJar.get(fileInOriginal.getKey());
            if (fileInNewJar == null) {
                log.info("File {} is removed", fileInOriginal.getKey());
                return MAJOR;
            }
            if (fileInNewJar.getCrc() != fileInOriginal.getValue().getCrc()) {
                log.info("File {} has been changed", fileInOriginal.getKey());
                result = updateResult(result, PATCH);
            }
        }

        for (Map.Entry<String, JarEntry> fileInNewJar : filesInNewJar.entrySet()) {
            if (configuration.isFileExcluded(fileInNewJar.getKey())) {
                continue;
            }
            JarEntry fileInOriginal = filesInOriginalJar.get(fileInNewJar.getKey());
            if (fileInOriginal == null) {
                log.info("File {} is added", fileInNewJar.getKey());
                result = updateResult(result, PATCH);
            }
        }
        return NONE;
    }

    private SemVerType determineClassDifference(ClassInformation originalClass, ClassInformation classInNewJar) {
        SemVerType classResult = NONE;
        try {
            Map<Constructor, Annotation[]> originalClassConstructors = originalClass.getConstructorsAndAnnotations();
            Map<Constructor, Annotation[]> classInNewJarConstructors = classInNewJar.getConstructorsAndAnnotations();
            SemVerType constructorResult = getSemVerType(originalClass.getClazz(), originalClassConstructors, classInNewJarConstructors);
            classResult = updateResult(classResult, constructorResult);

            Map<Field, Annotation[]> originalClassFields = originalClass.getFieldsAndAnnotations();
            Map<Field, Annotation[]> inNewJarFields = classInNewJar.getFieldsAndAnnotations();
            SemVerType fieldResult = getSemVerType(originalClass.getClazz(), originalClassFields, inNewJarFields);
            classResult = updateResult(classResult, fieldResult);

            Map<Method, Annotation[]> originalClassMethods = originalClass.getMethodsAndAnnotations();
            Map<Method, Annotation[]> classInNewJarMethods = classInNewJar.getMethodsAndAnnotations();
            SemVerType methodResult = getSemVerType(originalClass.getClazz(), originalClassMethods, classInNewJarMethods);
            classResult = updateResult(classResult, methodResult);
        } catch (LinkageError e) {
            log.info("Unable to determine class difference due to linkage error {}", e.getMessage());
            return MAJOR;
        }

        return classResult;
    }

    private int getVersionOfClass(JarFile jarFile) {
        return ClassVersion.getVersionNumber(jarFile);
    }

    private <M extends AccessibleObject> SemVerType getSemVerType(Class originalClass, Map<M, Annotation[]> originalClassMembers, Map<M, Annotation[]> inNewJarMembers) {
        SemVerType maxChange = NONE;
        for (M originalClassMember : originalClassMembers.keySet()) {
            M memberInNew = getMemberInOther(originalClassMember, inNewJarMembers);
            if (memberInNew == null) {
                log.info("{} '{}' no longer exists in {}", originalClassMember.getClass().getSimpleName(), originalClassMember, originalClass.getName());
                return MAJOR;
            } else {
                Annotation[] annotationsInNew = memberInNew.getAnnotations();
                Annotation[] annotationsInOriginal = originalClassMember.getAnnotations();
                SemVerType annotationSemVerType = getSemVerType(originalClassMember, annotationsInNew, annotationsInOriginal);
                maxChange = SemVerType.updateResult(maxChange, annotationSemVerType);
            }
        }

        if (maxChange != NONE) {
            return maxChange;
        }

        for (M memberInNew : inNewJarMembers.keySet()) {
            M originalClassMember = getMemberInOther(memberInNew, originalClassMembers);
            if (originalClassMember == null) {
                log.info("{} '{}' in {} is new", memberInNew.getClass().getSimpleName(), memberInNew, originalClass.getName());
                return MINOR;
            }
        }
        return NONE;
    }

    private SemVerType getSemVerType(AccessibleObject originalClassMember, Annotation[] annotationsInNew, Annotation[] annotationsInOriginal) {
        for (Annotation annotationInOriginal : annotationsInOriginal) {
            boolean foundInNew = false;
            for (Annotation annotationInNew : annotationsInNew) {
                if (annotationInOriginal.toString().equals(annotationInNew.toString())) {
                    foundInNew = true;
                    break;
                }
            }
            if (!foundInNew) {
                log.info("Annotation {} is not available on {}", annotationInOriginal.toString(), originalClassMember);
                return configuration.getAnnotationRemovedStrategy();
            }
        }
        for (Annotation annotationInNew : annotationsInNew) {
            boolean foundInOriginal = false;
            for (Annotation annotationInOriginal : annotationsInOriginal) {
                if (annotationInOriginal.toString().equals(annotationInNew.toString())) {
                    foundInOriginal = true;
                    break;
                }
            }
            if (!foundInOriginal) {
                log.info("Annotation {} is has been added on {}", annotationInNew.toString(), originalClassMember);
                return configuration.getAnnotationAddedStrategy();
            }
        }
        return NONE;
    }

    private <M extends AccessibleObject> M getMemberInOther(M originalClassMember, Map<M, Annotation[]> inNewJarMembers) {
        for (M method : inNewJarMembers.keySet()) {
            if (originalClassMember.toString().equals(method.toString())) {
                return method;
            }
        }
        return null;
    }

    private Set<ClassInformation> getClassesInJar(JarFile jarFile) throws IOException {
        List<URL> jarsInRuntime = new ArrayList<>(configuration.getRuntimeClasspathElements()
                .stream()
                .filter(jar -> jar.endsWith(".jar"))
                .map(s -> {
                    try {
                        return new URL("jar:file:" + s + "!/");
                    } catch (MalformedURLException e) {
                        return null;
                    }
                })
                .collect(Collectors.toList()));
        jarsInRuntime.add(new URL("jar:file:" + jarFile.getName() + "!/"));
        Set<String> classNames = getClassNamesFromJarFile(jarFile);
        Set<ClassInformation> classes = new HashSet<>(classNames.size());
        try (URLClassLoader cl = URLClassLoader.newInstance(jarsInRuntime.toArray(new URL[0]))) {
            for (String name : classNames) {
                if (MODULE_INFO_CLASS_NAME.equals(name)) {
                    log.debug("Skipping module-info as that is not a class");
                    continue;
                }
                try {
                    Class aClass = cl.loadClass(name);
                    Map<Constructor, Annotation[]> constructorsAndAnnotations = getAnnotationMapFromAccessibleObject(aClass.getConstructors());
                    Map<Method, Annotation[]> fieldsAndAnnotations = getAnnotationMapFromAccessibleObject(aClass.getMethods());
                    Map<Field, Annotation[]> methodsAndAnnotations = getAnnotationMapFromAccessibleObject(aClass.getFields());
                    classes.add(new ClassInformation(aClass, constructorsAndAnnotations, methodsAndAnnotations, fieldsAndAnnotations));
                } catch (ReflectiveOperationException | LinkageError e) {
                    log.warn("Failed to load class {} from {} due to: {} {}", name, jarFile.getName(), e.getClass().getName(), e.getMessage());
                }
            }
        }
        return classes;
    }

    private <T extends AccessibleObject> Map<T, Annotation[]> getAnnotationMapFromAccessibleObject(T[] AccessibleObjects) {
        Map<T, Annotation[]> tHashMap = new HashMap<>();
        for (T a : AccessibleObjects) {
            tHashMap.put(a, a.getAnnotations());
        }
        return tHashMap;
    }

    private Set<String> getClassNamesFromJarFile(JarFile jarFile) {
        return jarFile.stream()
                .filter(jar -> jar.getName().endsWith(".class"))
                .map(jar -> jar.getName().replace("/", ".")
                        .replace(".class", ""))
                .collect(Collectors.toSet());
    }

    private Map<String, JarEntry> getFilesNotClassedInJar(JarFile jarFile) {
        return jarFile.stream()
                .filter(jarEntry -> !jarEntry.getName().endsWith(".class"))
                .filter(jarEntry -> !jarEntry.getName().startsWith("META-INF/maven/"))
                .filter(jarEntry -> !jarEntry.isDirectory())
                .collect(Collectors.toMap(JarEntry::getName, Function.identity()));
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
