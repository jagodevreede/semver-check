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
import java.util.Map;

public class TestDataGenerator {

    private final StringWriter writer = new StringWriter();
    private final PrintWriter out = new PrintWriter(writer);
    private final String className;

    TestDataGenerator(String className) {
        this.className = className;
        out.print("public class ");
        out.print(className);
        out.println(" {");
    }

    public void compileClass() {
        out.println("}");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        JavaFileObject file = new JavaSourceFromString(className, writer.toString());

        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
        CompilationTask task = compiler.getTask(null, null, null, null, null, compilationUnits);

        task.call();
    }

    public void writeFile(String extension) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(className + extension))) {
            writer.write(writer.toString());
        }
    }

    public void addClassToJar(File jarFile) throws IOException {
        addFileToJar(jarFile, ".class");
    }

    public void addFileToJar(File jarFile, String extension) throws IOException {
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        URI uri = URI.create("jar:file:" + jarFile.getAbsolutePath());

        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            Path externalTxtFile = Paths.get(className + extension);
            Path pathInZipfile = zipfs.getPath("/" + className + extension);

            Files.copy(externalTxtFile, pathInZipfile, StandardCopyOption.REPLACE_EXISTING);
            Files.delete(externalTxtFile);
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