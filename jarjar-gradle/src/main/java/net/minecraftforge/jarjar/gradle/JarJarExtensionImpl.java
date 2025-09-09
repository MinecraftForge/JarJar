/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;
import net.minecraftforge.gradleutils.shared.Closures;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

abstract class JarJarExtensionImpl implements JarJarExtensionInternal {
    private final Project project;

    private @Nullable JarJarContainer container;

    private final NamedDomainObjectContainer<JarJarContainerInternal> containers = this.getObjects().domainObjectContainer(JarJarContainerInternal.class);

    private final JarJarProblems problems = this.getObjects().newInstance(JarJarProblems.class);
    protected abstract @Inject ObjectFactory getObjects();
    protected abstract @Inject ProviderFactory getProviders();

    @Inject
    public JarJarExtensionImpl(Project project) {
        this.project = project;
    }

    @Override
    public JarJarContainer getContainer() {
        return this.container != null ? this.container : this.register();
    }

    @Override
    public JarJarContainer register(String name, Action<? super JarJar> taskAction) {
        return this.register(name, this.project.getTasks().named("jar", org.gradle.api.tasks.bundling.Jar.class), taskAction);
    }

    @Override
    public JarJarContainer register(String name, TaskProvider<? extends Jar> jarTask, Action<? super JarJar> taskAction) {
        var container = this.getObjects().newInstance(JarJarContainerImpl.class, name, this.project, jarTask, taskAction);
        this.containers.add(container);
        return this.container = container;
    }

    @Override
    public void configure(Dependency dependency, Action<? super JarJarMetadataInfo> action) {
        JarJarDependencyMethods.jarJar(dependency, action);
    }
}
