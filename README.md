# SemVer check

This maven plugin allows you to check (or determine) the next version of your module, based on the rules
of [Semantic Versioning](https://semver.org/).

## Prerequisites

This plugin requires Maven 3.3.9 and Java 11 or higher to be able to run.

## Usage

There is an [example project](semver-check-maven-plugin-example/) that has a minium configuration.

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
                <failOnMissingFile>true</failOnMissingFile>
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

This plugin can be used with a multimodule project, an example project can be found [here]](semver-check-maven-plugin-multi-module-example/).

## Configuration

The following configuration options are available:

| Property name     | Default value     | Description                                                                                                                                                                           |
|-------------------|-------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| skip              | `false`           | If set to `true` then the build will skip the execution of this plugin                                                                                                                |
| failOnMissingFile | `false`           | If set to `true` then the build will fail if the plugin can't find the generated artifact                                                                                             |
| ignoreSnapshots   | `true`            | If set to `false` then the plugin will also compare to SNAPSHOT versions if it can find any (in local repo's for example)                                                             |
| outputFileName    | `nextVersion.txt` | The name of the file where the next version in plain text will be written to. This file is located in the `target` folder. If the property is left empty then no file will be created | 

## Getting involved

If you have questions, concerns, bug reports, etc, please file an issue in this repository's Issue Tracker, or better
yet create a Pull Request

If you contribute to this project please follow
the [conventional commits](https://www.conventionalcommits.org/en/v1.0.0/) specification for commit messages.

## How to release

**Prerequisites:**

- Have pgp keys getup on your machine
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

----

## Open source licensing info

1. [LICENSE](LICENSE)

----
