package io.github.jagodevreede.semver.check.core;

public enum SemVerType {
    MAJOR,
    MINOR,
    PATCH,
    NONE;

    public static SemVerType updateResult(SemVerType was, SemVerType toBe) {
        if (was.ordinal() < toBe.ordinal()) {
            return was;
        }
        return toBe;
    }
}
