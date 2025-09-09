/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

import net.minecraftforge.gradleutils.shared.EnhancedPlugin;
import net.minecraftforge.gradleutils.shared.EnhancedTask;
import org.gradle.api.Project;

interface JarJarTask extends EnhancedTask<JarJarProblems> {
    @Override
    default Class<? extends EnhancedPlugin<? super Project>> pluginType() {
        return JarJarPlugin.class;
    }

    @Override
    default Class<JarJarProblems> problemsType() {
        return JarJarProblems.class;
    }
}
