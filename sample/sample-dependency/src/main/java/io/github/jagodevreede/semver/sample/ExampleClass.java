package io.github.jagodevreede.semver.sample;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

public class ExampleClass extends ObjectIdGenerators {
    public static final String A_CONST = "abc";

    public ExampleClass() {

    }

    @JsonIgnore
    public void aMethod() {

    }

    protected void aProtectedMethod() {
        aPrivateMethod();
    }

    private void aPrivateMethod() {

    }

    @Deprecated
    private void aPrivateMethodThatWillBeRemoved() {

    }

}
