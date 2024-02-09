package io.github.jagodevreede.semver.check.core;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestDataGenerator {

    private final StringWriter writer = new StringWriter();
    private final PrintWriter out = new PrintWriter(writer);
    private final String className;
    private final String packageName;

    TestDataGenerator(String packageName, String className) {
        this.packageName = packageName;
        this.className = className;
        out.print("package ");
        out.print(packageName);
        out.println(";");
        initClass(className);
    }

    TestDataGenerator(String className) {
        this.packageName = "";
        this.className = className;
        initClass(className);
    }

    private void initClass(String className) {
        out.print("public class ");
        out.print(className);
        out.println(" {");
    }

    public void compileClass() {
        out.println("}");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        JavaFileObject file = new JavaSourceFromString(className, writer.toString());

        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
        CompilationTask task = compiler.getTask(null, null, null, List.of("--release", "11"), null, compilationUnits);

        task.call();
    }

    public void writeFile(String extension) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(className + extension))) {
            writer.write(writer.toString());
        }
    }

    public void addClassToJar(File jarFile) throws IOException {
        String packageFolder = packageName.replace('.', '/') + "/";
        packageFolder = packageFolder.replace("//", "/");
        addFileToJar(jarFile, ".class", packageFolder);
    }

    public void addFileToJar(File jarFile, String extension, String packageFolder) throws IOException {
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        URI uri = URI.create("jar:file:" + jarFile.getAbsolutePath());

        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            Path externalTxtFile = Paths.get(className + extension);
            Path folderToPlaceIn = zipfs.getPath(packageFolder);
            Path pathInZipfile = zipfs.getPath(packageFolder + className + extension);

            createFolders(folderToPlaceIn);

            Files.copy(externalTxtFile, pathInZipfile, StandardCopyOption.REPLACE_EXISTING);
            Files.delete(externalTxtFile);
        }
    }

    private static void createFolders(Path folderToPlaceIn) throws IOException {
        if (Files.notExists(folderToPlaceIn)) {
            if (folderToPlaceIn.getParent() != null && Files.notExists(folderToPlaceIn.getParent())) {
                createFolders(folderToPlaceIn.getParent());
            }
            Files.createDirectory(folderToPlaceIn);
        }
    }


    public void add(String something) {
        out.println(something);
        out.println();
    }
}

class JavaSourceFromString extends SimpleJavaFileObject {
    final String code;

    JavaSourceFromString(String name, String code) {
        super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
        this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }
}