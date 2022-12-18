package io.github.jagodevreede.semver.check.maven;

class HaltException extends Exception {
    HaltException(String message) {
        super(message);
    }
}
