/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

import org.gradle.api.Action;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;

/// Provides access to Forge Jar-in-Jar within [org.gradle.api.Project] buildscripts.
public sealed interface JarJarExtension extends JarJarContainer permits JarJarExtensionInternal {
    String NAME = JarJarPlugin.NAME;

    default JarJarContainer register() {
        return this.register(NAME);
    }

    default JarJarContainer register(String name) {
        return this.register(name, jarJar -> { });
    }

    default JarJarContainer register(TaskProvider<? extends Jar> jarTask) {
        return this.register(NAME, jarTask);
    }

    default JarJarContainer register(Action<? super JarJar> taskAction) {
        return this.register(NAME, taskAction);
    }

    default JarJarContainer register(String name, TaskProvider<? extends Jar> jarTask) {
        return this.register(name, jarTask, jarJar -> { });
    }

    JarJarContainer register(String name, Action<? super JarJar> taskAction);

    JarJarContainer register(String name, TaskProvider<? extends Jar> jarTask, Action<? super JarJar> taskAction);

    void configure(Dependency dependency, Action<? super JarJarMetadataInfo> action);
}
