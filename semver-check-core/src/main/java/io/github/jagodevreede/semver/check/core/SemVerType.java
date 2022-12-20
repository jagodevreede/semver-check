package io.github.jagodevreede.semver.check.core;

/**
 * This is an enum that represents the different types of version updates that can be made to a software project,
 * according to the semantic versioning (SemVer) specification.
 */
public enum SemVerType {
    /**
     * A major version update indicates a significant change that is not backwards compatible with the previous version.
     * The major version number is incremented when this type of change is made.
     */
    MAJOR,
    /**
     * A minor version update indicates a backwards-compatible feature addition or change. The minor version number is
     * incremented when this type of change is made.
     */
    MINOR,
    /**
     * A patch version update indicates a backwards-compatible bug fix. The patch version number is incremented when this
     * type of change is made.
     */
    PATCH,
    /**
     * No version update.
     */
    NONE;

    /**
     * @param was the SemVerType value to compare
     * @param toBe the SemVerType value to compare
     * @return the SemVerType value with value representing the most significant change
     */
    public static SemVerType updateResult(SemVerType was, SemVerType toBe) {
        if (was.ordinal() < toBe.ordinal()) {
            return was;
        }
        return toBe;
    }
}
