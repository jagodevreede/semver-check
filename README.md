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
            <groupId>com.github.jagodevreede</groupId>
            <version>VERSION_NUMBER</version>
            <configuration>
                <failOnMissingFile>true</failOnMissingFile>
                <outputToFile>${build.directory}/nextversion.txt</outputToFile>
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

## Configuration

The following configuration options are available:

| Property name     | Default value | Description                                                                                                                                                                                                               |
|-------------------|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| failOnMissingFile | `false`       | If set to `true` then the build will fail if the plugin can't find the generated artifact                                                                                                                                 |
| ignoreSnapshots   | `true`        | If set to `false` then the plugin will also compare to SNAPSHOT versions if it can find any (in local repo's for example)                                                                                                 |
| outputToFile      |               | The value of the property is the location where a txt file will be writen to that contains the suggested next version. For example `${build.directory}/nextversion.txt` will put a `nextversion.txt` in you target folder | 

## Getting involved

If you have questions, concerns, bug reports, etc, please file an issue in this repository's Issue Tracker, or better yet create a Pull Request

----

## Open source licensing info

1. [LICENSE](LICENSE)

----
