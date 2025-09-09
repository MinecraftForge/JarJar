/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ResolvableConfiguration;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.attributes.java.TargetJvmVersion;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.ConfigurationVariantDetails;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import javax.inject.Inject;

abstract class JarJarContainerImpl implements JarJarContainerInternal {
    private final String name;
    private final Project project;
    private final TaskProvider<? extends Jar> jar;

    private final TaskProvider<JarJar> task;
    private final NamedDomainObjectProvider<DependencyScopeConfiguration> configuration;
    private final NamedDomainObjectProvider<ResolvableConfiguration> resolvableConfiguration;
    private final NamedDomainObjectProvider<AdhocComponentWithVariants> softwareComponent;

    private final JarJarProblems problems = this.getObjects().newInstance(JarJarProblems.class);
    protected abstract @Inject ObjectFactory getObjects();
    protected abstract @Inject ProviderFactory getProviders();

    @Inject
    public JarJarContainerImpl(String name, Project project, TaskProvider<? extends Jar> jar, Action<? super JarJar> taskAction) {
        this.name = name;
        this.project = project;
        this.jar = jar;

        var java = project.getExtensions().getByType(JavaPluginExtension.class);
        var configurations = project.getConfigurations();
        try {
            this.configuration = configurations.dependencyScope(this.getConfigurationName(), c ->
                c.setDescription("The default configuration to be used for Forge Jar-in-Jar's output.")
            );
            this.resolvableConfiguration = configurations.resolvable(this.getResolvableConfigurationName(), c -> {
                c.setDescription("The default resolvable configuration to be used in Forge Jar-in-Jar's output.");
                c.extendsFrom(this.configuration.get());
            });
        } catch (InvalidUserDataException e) {
            throw problems.configurationAlreadyExists(e);
        }

        this.task = JarJar.register(this, taskAction);

        var runtimeClasspath = configurations.named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
        var jarJarRuntimeElements = configurations.consumable(this.getConsumableConfigurationName(), configuration -> {
            configuration.attributes(attributes -> {
                attributes.attribute(Usage.USAGE_ATTRIBUTE, this.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, this.getObjects().named(Category.class, Category.LIBRARY));
                attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, this.getObjects().named(LibraryElements.class, LibraryElements.JAR));
                attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, this.getObjects().named(Bundling.class, Bundling.SHADOWED));

                attributes.attributeProvider(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, configurations
                    .named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)
                    .map(c -> c.getAttributes().getAttribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE))
                    .orElse(this.getProviders().provider(() -> java.getTargetCompatibility().getMajorVersion()).map(JavaLanguageVersion::of).map(JavaLanguageVersion::asInt)));
            });
            configuration.outgoing(outgoing -> outgoing.artifact(task));
            configuration.withDependencies(dependencies -> {
                var included = this.resolvableConfiguration.get().getAllDependencies();
                for (var dependency : runtimeClasspath.get().getAllDependencies()) {
                    if (!included.contains(dependency))
                        dependencies.add(dependency);
                }
            });
        });

        this.softwareComponent = project.getComponents().register(this.getSoftwareComponentName(), AdhocComponentWithVariants.class, component -> {
            component.addVariantsFromConfiguration(jarJarRuntimeElements.get(), variant -> {
                variant.getConfigurationVariant().getDescription().set("Dependencies shadowed using Forge Jar-in-Jar.");
                variant.mapToMavenScope("runtime");
            });
        });

        project.getComponents().named("java", AdhocComponentWithVariants.class, component -> {
            component.addVariantsFromConfiguration(jarJarRuntimeElements.get(), ConfigurationVariantDetails::mapToOptional);
        });

        project.afterEvaluate(this::finish);
    }

    private void finish(Project project) {
        var jarJarConfiguration = this.configuration.get();
        if (jarJarConfiguration.getDependencies().size() != jarJarConfiguration.getAllDependencies().size())
            this.problems.reportConfigurationWithTransitiveDependencies();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Project getProject() {
        return this.project;
    }

    @Override
    public TaskProvider<? extends Jar> getJar() {
        return this.jar;
    }

    @Override
    public NamedDomainObjectProvider<DependencyScopeConfiguration> getConfiguration() {
        return this.configuration;
    }

    @Override
    public NamedDomainObjectProvider<ResolvableConfiguration> getResolvableConfiguration() {
        return this.resolvableConfiguration;
    }

    @Override
    public NamedDomainObjectProvider<AdhocComponentWithVariants> getSoftwareComponent() {
        return this.softwareComponent;
    }
}
