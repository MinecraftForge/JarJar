/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvableConfiguration;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;

public interface JarJarContainerInternal extends JarJarContainer, Named, HasPublicType {
    static JarJarContainer register(String name, Project project, TaskProvider<? extends Jar> jar, Action<? super JarJar> taskAction) {
        return project.getObjects().newInstance(JarJarContainerImpl.class, name, project, jar, taskAction);
    }

    @Override
    default String getConfigurationName() {
        return this.getName();
    }

    @Override
    default String getConsumableConfigurationName() {
        return this.getConfigurationName() + "RuntimeElements";
    }

    default String getConsumableDependenciesConfigurationName() {
        return this.getConfigurationName() + "RuntimeDependencies";
    }

    @Override
    default String getSoftwareComponentName() {
        return this.getName();
    }

    default String getResolvableConfigurationName() {
        return this.getName() + "Classpath";
    }

    Project getProject();

    TaskProvider<? extends Jar> getJar();

    NamedDomainObjectProvider<ResolvableConfiguration> getResolvableConfiguration();

    @Override
    default TypeOf<?> getPublicType() {
        return TypeOf.typeOf(JarJarContainer.class);
    }
}
