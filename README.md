# SemVer check

![GitHub](https://img.shields.io/github/license/jagodevreede/semver-check)
![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/jagodevreede/semver-check?label=Latest%20release)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/jagodevreede/semver-check/maven.yml?branch=main)

This maven plugin allows you to check (or determine) the next version of your module, based on the rules
of [Semantic Versioning](https://semver.org/).

## Prerequisites

This plugin requires Maven 3.3.9 and Java 11 or higher to be able to run.

## Usage

There is an [example project](semver-check-maven-plugin-example) that has a minimum configuration but all options
specified.

Add the following configuration to your `pom.xml` and set the VERSION_NUMBER to the latest version released.

```xml

<build>
    ...
    <plugins>
        ...
        <plugin>
            <artifactId>semver-check-maven-plugin</artifactId>
            <groupId>io.github.jagodevreede</groupId>
            <version>VERSION_NUMBER</version>
            <configuration>
                <haltOnFailure>true</haltOnFailure>
                <outputFileName>nextVersion.txt</outputFileName>
            </configuration>
            <executions>
                <execution>
                    <id>check</id>
                    <phase>verify</phase>
                    <goals>
                        <goal>check</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
        ...
    </plugins>
    ...
</build>
```

This plugin can be used with a multimodule project, an example project can be found [here](https://github.com/jagodevreede/semver-check/tree/main/semver-check-maven-plugin-multi-module-example).

## Configuration

The following configuration options are available:

| Property name          | Default value     | Description                                                                                                                                                                           |
|------------------------|-------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| skip                   | `false`           | If set to `true` then the build will skip the execution of this plugin                                                                                                                |
| skipDependencyCheck    | `false`           | If set to `true` then the dependencies will not be compared to the previous version                                                                                                   |
| haltOnFailure          | `true`            | If set to `false` then the build will not fail if the plugin encounter a problem, but only log a warning                                                                              |
| ignoreSnapshots        | `true`            | If set to `false` then the plugin will also compare to SNAPSHOT versions if it can find any (in local repo's for example)                                                             |
| outputFileName         | `nextVersion.txt` | The name of the file where the next version in plain text will be written to. This file is located in the `target` folder. If the property is left empty then no file will be created |
| writeFileOnNone        | `true`            | If set to `false` then the output file will not be written if the determined version upgrade type is `none`                                                                           |
| overwriteOutputFile    | `true`            | If set to `false` then the output file will not be overwritten.                                                                                                                       |
| excludePackages        |                   | Ignores packages can be a comma separated list or a list of excludePackage (See [example project](semver-check-maven-plugin-example) for example)                                     |
| excludeFiles           |                   | Ignores files in that starts with given here. Can be a comma separated list or a list of excludeFile (See [example project](semver-check-maven-plugin-example) for example)           |
| failOnIncorrectVersion | `false`           | If set to `true` then if the semver mismatches the build will fail.                                                                                                                   |
| allowHigherVersions    | `true`            | Only has effect when `failOnIncorrectVersion` is set.  If allowHigherVersions set to `false` it will also break if it detected a is lower then expected version.                      |

## How is the semver type determined

### Major

- Changing the java version of the compiled classes to a higher version.
- Removal of a public class, method, field or static variable.
- Removal of a resource file
- Removal of an annotation on a public API

### Minor

- Addition of a public class, method, annotation, field or static variable.
- Removal of an annotation on a non public API

## Patch

- Any change that changes the byte code
- Any change in a resource file (Note that files in META-INF/maven/ are ignored as they are generated by maven)
- Any change in a dependency

## Getting involved

If you have questions, concerns, bug reports, etc, please file an issue in this repository's Issue Tracker, or better
yet create a Pull Request

If you contribute to this project please follow
the [conventional commits](https://www.conventionalcommits.org/en/v1.0.0/) specification for commit messages.

## How to release

All automated with the github release action.

**Prerequisites:**

- Have pgp keys setup on your machine
- Have access to the group id on https://s01.oss.sonatype.org/
- Have [jreleaser](https://jreleaser.org/guide/latest/install.html) installed

First set the correct version to be released:

> ```mvn versions:set -DnewVersion=1.2.3```

Tag this release with tag v1.2.3

Then stage the release:

> ```mvn -Ppublication clean deploy -DaltDeploymentRepository=local::default::file://`pwd`/target/staging-deploy```

The start the actual release:

> ```mvn -Ppublication jreleaser:full-release```
> Add dry drun if you first need to check what it will do:
> `-Djreleaser.dry.run=true`

Lastly set the next snapshot version:

> ```mvn versions:set -DnewVersion=1.2.4-SNAPSHOT```

Close the release in maven central at https://s01.oss.sonatype.org/#stagingRepositories

----

## Open source licensing info

1. [LICENSE](LICENSE)

----
