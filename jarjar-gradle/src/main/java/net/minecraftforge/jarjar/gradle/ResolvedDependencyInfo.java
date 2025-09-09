/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;

import javax.inject.Inject;
import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

final class ResolvedDependencyInfo implements Serializable {
    private static final @Serial long serialVersionUID = -7577318115877822993L;

    final ModuleVersionIdentifier module;
    final String version;
    final String versionRange;
    final boolean hasManuallySpecifiedRange;
    final File artifact;
    final String asString;

    @Inject
    public ResolvedDependencyInfo(ModuleVersionIdentifier module, String version, String versionRange,
                                  boolean hasManuallySpecifiedRange, File artifact, String asString) {
        this.module = module;
        this.version = version;
        this.versionRange = versionRange;
        this.hasManuallySpecifiedRange = hasManuallySpecifiedRange;
        this.artifact = artifact;
        this.asString = asString;
    }

    static Set<File> getFiles(Set<ResolvedDependencyInfo> resolvedDependencies) {
        var ret = new HashSet<File>(resolvedDependencies.size());
        for (var dependency : resolvedDependencies) {
            ret.add(dependency.artifact);
        }
        return ret;
    }

    static Set<ResolvedDependencyInfo> from(JarJarProblems problems, ConfigurationContainer configurations, Configuration configuration) {
        var dependencies = configuration.getAllDependencies();
        if (dependencies.isEmpty())
            return Set.of();

        var ret = new HashSet<ResolvedDependencyInfo>(dependencies.size());

        for (var dependency : dependencies) {
            var jarJar = (JarJarMetadataInfoInternal) JarJarDependencyMethods.getJarJar(dependency);
            var group = jarJar.getGroup();
            var name = jarJar.getName();
            var version = jarJar.getVersion();
            var versionRange = jarJar.getRange();
            var hasManuallySpecifiedRange = jarJar.hasManuallySpecifiedRange();

            if (dependency instanceof FileCollectionDependency filesDependency) {
                File artifact;
                try {
                    artifact = filesDependency.getFiles().getSingleFile();
                } catch (IllegalStateException e) {
                    // TODO fileCollectionDependencyIsNotSingleFile
                    throw e;
                }

                ret.add(new ResolvedDependencyInfo(
                    new MinimalModuleVersionIdentifier(group, name, version),
                    version,
                    versionRange,
                    hasManuallySpecifiedRange,
                    artifact,
                    Util.toString(filesDependency)
                ));
            } else if (dependency instanceof ModuleDependency moduleDependency) {
                moduleDependency = moduleDependency.copy();
                if (moduleDependency instanceof ExternalModuleDependency externalModuleDependency) {
                    externalModuleDependency.version(v -> v.strictly(version.toString()));
                }

                var detachedConfiguration = configurations.detachedConfiguration(moduleDependency);
                detachedConfiguration.setTransitive(false);

                boolean hasInfo = false;
                for (var artifact : detachedConfiguration.getResolvedConfiguration().getFirstLevelModuleDependencies().iterator().next().getModuleArtifacts()) {
                    var fileName = getFileName(artifact);
                    if (!fileName.endsWith(".jar"))
                        continue;

                    if (hasInfo)
                        throw problems.moduleHasTooManyJarArtifacts(moduleDependency, artifact);

                    ret.add(new ResolvedDependencyInfo(
                        new MinimalModuleVersionIdentifier(group, name, artifact.getModuleVersion().getId().getVersion()),
                        version,
                        versionRange,
                        hasManuallySpecifiedRange,
                        artifact.getFile(),
                        Util.toString(moduleDependency)
                    ));
                    hasInfo = true;
                }
                if (!hasInfo)
                    throw problems.moduleHasNoJarArtifacts(moduleDependency);
            } else {
                throw problems.dependencyIsNotAModule(dependency);
            }
        }

        return ret;
    }

    private static String getFileName(ResolvedArtifact artifact) {
        try {
            return InvokerHelper.getProperty(artifact.getId(), "fileName").toString();
        } catch (Throwable e) {
            // NOTE: Why not just use this to begin with?
            // ComponentArtifactIdentifier can have a getFileName() method, which doesn't necessarily resolve the file itself.
            // This allows us to get the name of the file to be used without asking Gradle to download the file.
            // So, if a file is not a JAR file, we can check the name without actually downloading it.
            return artifact.getFile().getName();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ResolvedDependencyInfo) obj;
        return Objects.equals(this.module, that.module) &&
            Objects.equals(this.version, that.version) &&
            Objects.equals(this.versionRange, that.versionRange) &&
            Objects.equals(this.artifact, that.artifact);
    }

    @Override
    public int hashCode() {
        return Objects.hash(module, version, versionRange, artifact);
    }

    @Override
    public String toString() {
        return "ResolvedDependencyInfo[" +
            "module=" + module + ", " +
            "fixedVersion=" + version + ", " +
            "versionRange=" + versionRange + ", " +
            "artifact=" + artifact + ']';
    }


    static final class MinimalModuleVersionIdentifier implements ModuleIdentifier, ModuleVersionIdentifier {
        private static final @Serial long serialVersionUID = -955346236759069739L;

        private final String group;
        private final String name;
        private final String version;

        @Inject
        public MinimalModuleVersionIdentifier(String group, String name, String version) {
            this.group = group;
            this.name = name;
            this.version = version;
        }

        @Override
        public ModuleIdentifier getModule() {
            return this;
        }

        @Override
        public String getGroup() {
            return this.group;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getVersion() {
            return this.version;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof MinimalModuleVersionIdentifier o
                && Objects.equals(this.group, o.group)
                && Objects.equals(this.name, o.name)
                && Objects.equals(this.version, o.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(group, name, version);
        }

        @Override
        public String toString() {
            return "MinimalModuleVersionIdentifier[" +
                "group=" + group + ", " +
                "name=" + name + ", " +
                "version=" + version + ']';
        }
    }
}
