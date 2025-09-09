/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;

@VisibleForTesting
@ApiStatus.Experimental
public abstract class JarJar extends org.gradle.api.tasks.bundling.Jar implements JarJarTask {
    static TaskProvider<JarJar> register(JarJarContainerInternal container, Action<? super JarJar> taskAction) {
        var project = container.getProject();
        var jar = container.getJar();
        var tasks = project.getTasks();

        var jarJar = tasks.register(jar.getName() + "Jar", JarJar.class, task -> {
            task.setDescription("Combines an assembled jar archive with the resolved Jar-in-Jar dependencies.");
            task.setGroup(jar.map(Task::getGroup).getOrElse(BasePlugin.BUILD_GROUP));
            task.getArchiveClassifier().set(jar.flatMap(Jar::getArchiveClassifier).filter(s -> !s.isBlank()).map(s -> s + "-all").orElse("all"));

            task.dependsOn(jar);
            task.with(jar.get());

            task.setConfiguration(container.getResolvableConfiguration());

            taskAction.execute(task);
        });

        tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME::equals).configureEach(it -> it.dependsOn(jarJar));

        project.afterEvaluate(p -> {
            var metadata = p.getTasks().register(jarJar.getName() + "Metadata", JarJarMetadata.class, task -> {
                task.setDescription("Generates the Jar-in-Jar metadata to be used by task '%s'".formatted(jarJar.getName()));
                task.getResolvedDependencies().set(jarJar.get().resolvedDependencies);
            });

            jarJar.get().dependsOn(metadata);
            jarJar.get().getMetadataFile().set(metadata.flatMap(JarJarMetadata::getMetadataFile));
            jarJar.get().setManifest(jar.get().getManifest());
        });

        return jarJar;
    }

    private final CopySpec jarJarCopySpec = this.getMainSpec().addChild().into("META-INF/jarjar");

    protected abstract @InputFiles @Classpath @SkipWhenEmpty ConfigurableFileCollection getIncludedClasspath();

    final SetProperty<ResolvedDependencyInfo> resolvedDependencies = this.getObjectFactory().setProperty(ResolvedDependencyInfo.class);

    public void setConfiguration(Configuration configuration) {
        this.resolvedDependencies.set(this.getProviders().provider(() -> configuration).map(c -> ResolvedDependencyInfo.from(this.problems, this.getProject().getConfigurations(), c)));
        this.getIncludedClasspath().setFrom(this.resolvedDependencies.map(ResolvedDependencyInfo::getFiles));
    }

    // TODO figure out making this lazy?
    public void setConfiguration(Provider<? extends Configuration> configuration) {
        this.setConfiguration(configuration.get());
    }

    public abstract @InputFile RegularFileProperty getMetadataFile();

    private final JarJarProblems problems = this.getObjectFactory().newInstance(JarJarProblems.class);

    protected abstract @Inject ProviderFactory getProviders();

    public JarJar() { }

    @Override
    protected void copy() {
        this.jarJarCopySpec.from(this.getIncludedClasspath());
        this.jarJarCopySpec.from(this.getMetadataFile());
        super.copy();
    }
}
