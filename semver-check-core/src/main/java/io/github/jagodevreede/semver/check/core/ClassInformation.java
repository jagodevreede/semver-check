package io.github.jagodevreede.semver.check.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

@SuppressWarnings("rawtypes") // We don't care about types
final class ClassInformation {

    private final Class aClass;
    private final Map<Constructor, Annotation[]> constructorsAndAnnotations;
    private final Map<Field, Annotation[]> fieldsAndAnnotations;
    private final Map<Method, Annotation[]> methodsAndAnnotations;

    public ClassInformation(Class aClass, Map<Constructor, Annotation[]> constructorsAndAnnotations, Map<Field, Annotation[]> fieldsAndAnnotations, Map<Method, Annotation[]> methodsAndAnnotations) {
        this.aClass = aClass;
        this.constructorsAndAnnotations = constructorsAndAnnotations;
        this.fieldsAndAnnotations = fieldsAndAnnotations;
        this.methodsAndAnnotations = methodsAndAnnotations;
    }

    public Class getClazz() {
        return aClass;
    }

    public Constructor[] getConstructors() {
        return constructorsAndAnnotations.keySet().toArray(new Constructor[0]);
    }

    public Field[] getFields() {
        return fieldsAndAnnotations.keySet().toArray(new Field[0]);
    }

    public Method[] getMethods() {
        return methodsAndAnnotations.keySet().toArray(new Method[0]);
    }

    public Map<Constructor, Annotation[]> getConstructorsAndAnnotations() {
        return constructorsAndAnnotations;
    }

    public Map<Field, Annotation[]> getFieldsAndAnnotations() {
        return fieldsAndAnnotations;
    }

    public Map<Method, Annotation[]> getMethodsAndAnnotations() {
        return methodsAndAnnotations;
    }

    public String getName() {
        return aClass.getName();
    }
}
