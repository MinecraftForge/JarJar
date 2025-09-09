/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

import net.minecraftforge.jarjar.metadata.ContainedJarIdentifier;
import net.minecraftforge.jarjar.metadata.ContainedJarMetadata;
import net.minecraftforge.jarjar.metadata.ContainedVersion;
import net.minecraftforge.jarjar.metadata.Metadata;
import net.minecraftforge.jarjar.metadata.MetadataIOHandler;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Objects;

abstract class JarJarMetadata extends DefaultTask implements JarJarTask {
    protected abstract @Input SetProperty<ResolvedDependencyInfo> getResolvedDependencies();

    protected abstract @OutputFile RegularFileProperty getMetadataFile();

    protected abstract @Inject WorkerExecutor getWorkerExecutor();
    protected abstract @InputFiles @Classpath ConfigurableFileCollection getWorkerClasspath();

    @Inject
    public JarJarMetadata() {
        this.getMetadataFile().convention(this.getDefaultOutputDirectory().map(d -> d.file("metadata.json")));

        this.getWorkerClasspath().setFrom(this.getTool(Tools.JARJAR_LEGACY_METADATA));
    }

    @TaskAction
    protected void exec() {
        var work = this.getWorkerExecutor().classLoaderIsolation(spec ->
            spec.getClasspath().from(this.getWorkerClasspath())
        );

        work.submit(Action.class, parameters -> {
            parameters.getResolvedDependencies().set(this.getResolvedDependencies());
            parameters.getMetadataFile().set(this.getMetadataFile());
        });

        work.await();
    }

    static abstract class Action implements WorkAction<Action.Parameters> {
        interface Parameters extends WorkParameters {
            SetProperty<ResolvedDependencyInfo> getResolvedDependencies();

            RegularFileProperty getMetadataFile();
        }

        private final JarJarProblems problems = this.getObjects().newInstance(JarJarProblems.class);

        protected abstract @Inject ObjectFactory getObjects();

        @Inject
        public Action() { }

        @Override
        public void execute() {
            var parameters = this.getParameters();

            var resolved = parameters.getResolvedDependencies().get();
            var jars = new ArrayList<ContainedJarMetadata>(resolved.size());
            for (var dependency : resolved) {
                jars.add(new ContainedJarMetadata(
                    new ContainedJarIdentifier(validateGroup(dependency), dependency.module.getName()),
                    new ContainedVersion(parseVersionRange(dependency), parseVersion(dependency)),
                    "META-INF/jarjar/" + dependency.artifact.getName(),
                    false
                ));
            }

            try {
                Files.write(
                    parameters.getMetadataFile().getAsFile().get().toPath(),
                    MetadataIOHandler.toLines(new Metadata(jars))
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private String validateGroup(ResolvedDependencyInfo dependency) {
            try {
                return Objects.requireNonNull(dependency.module.getGroup());
            } catch (NullPointerException e) {
                throw problems.dependencyHasUnspecifiedModuleGroup(e, dependency.asString);
            }
        }

        private ArtifactVersion parseVersion(ResolvedDependencyInfo resolved) {
            try {
                return VersionRange.createFromVersionSpec(Objects.requireNonNull(resolved.version)).getRecommendedVersion();
            } catch (InvalidVersionSpecificationException e) {
                throw problems.dependencyHasInvalidVersion(e, resolved.asString);
            } catch (NullPointerException e) {
                throw problems.dependencyHasUnspecifiedVersion(e, resolved.asString);
            }
        }

        private VersionRange parseVersionRange(ResolvedDependencyInfo dependency) {
            if (dependency.hasManuallySpecifiedRange) {
                try {
                    return VersionRange.createFromVersionSpec(dependency.versionRange);
                } catch (InvalidVersionSpecificationException e) {
                    throw problems.dependencyHasInvalidVersionRange(e, dependency.asString);
                }
            } else {
                try {
                    return VersionRange.createFromVersionSpec("[%s,)".formatted(Objects.requireNonNull(dependency.versionRange)));
                } catch (InvalidVersionSpecificationException e) {
                    throw problems.dependencyHasInvalidVersionForRange(e, dependency.asString);
                } catch (NullPointerException e) {
                    throw problems.dependencyHasUnspecifiedVersionForRange(e, dependency.asString);
                }
            }
        }
    }
}
