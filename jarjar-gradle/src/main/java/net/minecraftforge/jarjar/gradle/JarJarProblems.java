/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

import net.minecraftforge.gradleutils.shared.EnhancedProblems;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.problems.Severity;

import javax.inject.Inject;
import java.io.Serial;

abstract class JarJarProblems extends EnhancedProblems {
    private static final @Serial long serialVersionUID = 1679007062098065431L;

    @Inject
    public JarJarProblems() {
        super(JarJarPlugin.NAME, JarJarPlugin.DISPLAY_NAME);
    }

    RuntimeException configurationAlreadyExists(InvalidUserDataException e) {
        return this.throwing(e, "jarjar-configuration-exists", "jarJar configuration already exists", spec -> spec
            .details("""
                Cannot create the jarJar configuration because it already exists.
                If you are using ForgeGradle 6, note that it is not compatible with this new plugin.""")
            .severity(Severity.ERROR)
            .solution("Do not use Forge Jar-in-Jar Gradle with ForgeGradle 6.")
            .solution(HELP_MESSAGE)
        );
    }

    void reportConfigurationWithTransitiveDependencies() {
        this.report("jarjar-configuration-transitive-deps", "jarJar configuration contains transitive dependencies", spec -> spec
            .details("""
                The jarJar configuration contains transitive dependencies.
                This is not currently fully supported and may cause issues with the built artifact.""")
            .severity(Severity.ADVICE)
            .solution("Do not use the jarJar configuration with transitive dependencies.")
            .solution(HELP_MESSAGE));
    }

    RuntimeException dependencyIsNotAModule(Dependency dependency) {
        return this.throwing(new IllegalArgumentException(), "jarjar-dependency-not-valid-type", "Dependency is not a module or a single file collection", spec -> spec
            .details("""
                A requested dependency for Forge Jar-in-Jar is not a module or a file collection containing a single file.
                The Jar-in-Jar selector uses module info at runtime for dependency management.
                This is an unrecoverable error as non-module dependencies are not supported by the Forge Jar-in-Jar specification.
                Dependency: %s""".formatted(Util.toString(dependency)))
            .severity(Severity.ERROR)
            .solution("Use a module dependency or use a dependency substitution to describe it as a module.")
            .solution("If using a local file, use a file collection dependency of that single file and use the 'jarJar' block inside your dependency declaration to describe the module.")
            .solution(HELP_MESSAGE));
    }

    RuntimeException moduleHasTooManyJarArtifacts(ModuleDependency dependency, ResolvedArtifact artifact) {
        return this.throwing(new IllegalArgumentException(), "jarjar-dependency-too-many-artifacts", "Dependency module has too many JAR artifacts", spec -> spec
            .details("""
                A requested dependency for Forge Jar-in-Jar contains more than one JAR artifact.
                This is not supported by the specification and is an unrecoverable error.
                Dependency: %s
                Offending Artifact: %s (%s)""".formatted(Util.toString(dependency), artifact.toString(), artifact.getFile().getName()))
            .severity(Severity.ERROR)
            .solution("Use variant-aware resolution using attributes to select a specific variant of the dependency that contains a single JAR file.")
            .solution("If you are using a file collection dependency, consider making a separate dependency for each JAR file.")
            .solution(HELP_MESSAGE));
    }

    RuntimeException moduleHasNoJarArtifacts(ModuleDependency dependency) {
        return this.throwing(new IllegalArgumentException(), "jarjar-dependency-too-many-artifacts", "Dependency module has too many JAR artifacts", spec -> spec
            .details("""
                A requested dependency for Forge Jar-in-Jar does not contain any JAR artifacts.
                This would cause the dependency to be included in the Jar-in-Jar output, but not to be read by the selector at runtime.
                This is an unrecoverable error, so as to avoid this issue going unnoticed.
                Note that when considering dependencies, artifacts that are not JAR files are ignored.
                Dependency: %s""".formatted(Util.toString(dependency)))
            .severity(Severity.ERROR)
            .solution("Use variant-aware resolution using attributes to select a specific variant of the dependency that contains a single JAR file.")
            .solution("If you are using a project dependency, configure the locally published artifact to be a JAR file.")
            .solution(HELP_MESSAGE));
    }

    RuntimeException dependencyHasUnspecifiedModuleGroup(NullPointerException e, String dependency) {
        return this.throwing(e, "jarjar-dependency-missing-module-group", "Dependency is missing module gorup", spec -> spec
            .details("""
                A requested dependency for Forge Jar-in-Jar does not have a module group.
                This is an unrecoverable error as the runtime selector needs a full module descriptor to handle dependencies.
                Dependency: %s""".formatted(dependency))
            .severity(Severity.ERROR)
            .solution("Use a dependency with a valid module, including a module group")
            .solution("Manually specify the group using the 'jarJar.module' block inside your dependency declaration.")
            .solution(HELP_MESSAGE));
    }

    RuntimeException dependencyHasInvalidVersion(Exception e, String dependency) {
        return this.throwing(e, "jarjar-dependency-invalid-version", "Dependency has invalid version", spec -> spec
            .details("""
                A requested dependency for Forge Jar-in-Jar has an invalid version that could not be parsed into a single Maven version.
                This is an unrecoverable error as the runtime selector would not be able to parse the version.
                Dependency: %s""".formatted(dependency))
            .severity(Severity.ERROR)
            .solution("Use a valid version for the dependency.")
            .solution("Manually specify the version using the 'jarJar' block inside your dependency declaration.")
            .solution(HELP_MESSAGE));
    }

    RuntimeException dependencyHasInvalidVersionForRange(Exception e, String dependency) {
        return this.throwing(e, "jarjar-dependency-invalid-version-range", "Dependency has invalid version", spec -> spec
            .details("""
                A requested dependency for Forge Jar-in-Jar has an invalid version that could not be parsed into a Maven version range.
                This is an unrecoverable error as the runtime selector would not be able to parse the version range.
                Dependency: %s""".formatted(dependency))
            .severity(Severity.ERROR)
            .solution("Use a valid version for the dependency.")
            .solution("Manually specify the version range using the 'jarJar' block inside your dependency declaration.")
            .solution(HELP_MESSAGE));
    }

    RuntimeException dependencyHasInvalidVersionRange(Exception e, String dependency) {
        return this.throwing(e, "jarjar-dependency-invalid-version-range-manual", "Dependency has invalid version", spec -> spec
            .details("""
                A requested dependency for Forge Jar-in-Jar has an invalid version range that could not be parsed into a Maven version range.
                This is an unrecoverable error as the runtime selector would not be able to parse the version range.
                Dependency: %s""".formatted(dependency))
            .severity(Severity.ERROR)
            .solution("Ensure that the specified version range is a valid Maven version range.")
            .solution(HELP_MESSAGE));
    }

    RuntimeException dependencyHasUnspecifiedVersion(NullPointerException e, String dependency) {
        return this.throwing(e, "jarjar-dependency-unspecified-version", "Dependency has missing version", spec -> spec
            .details("""
                A requested dependency for Forge Jar-in-Jar does not have a version.
                This is an unrecoverable error as the runtime selector would not have a version to work with.
                Dependency: %s""".formatted(dependency))
            .severity(Severity.ERROR)
            .solution("Specify a version for the dependency.")
            .solution("Manually specify the version using the 'jarJar' block inside your dependency declaration.")
            .solution(HELP_MESSAGE));
    }

    RuntimeException dependencyHasUnspecifiedVersionForRange(NullPointerException e, String dependency) {
        return this.throwing(e, "jarjar-dependency-unspecified-version-range", "Dependency has missing version", spec -> spec
            .details("""
                A requested dependency for Forge Jar-in-Jar does not have a version.
                This is an unrecoverable error as the runtime selector would not have a version range to work with.
                Dependency: %s""".formatted(dependency))
            .severity(Severity.ERROR)
            .solution("Specifu a version for the dependency.")
            .solution("Manually specify the version range using the 'jarJar' block inside your dependency declaration.")
            .solution(HELP_MESSAGE));
    }
}
