/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

import net.minecraftforge.gradleutils.shared.EnhancedPlugin;
import org.gradle.api.Project;

import javax.inject.Inject;

abstract class JarJarPlugin extends EnhancedPlugin<Project> {
    static final String NAME = "jarJar";
    static final String DISPLAY_NAME = "Forge Jar-in-Jar";

    @Inject
    public JarJarPlugin() {
        super(NAME, DISPLAY_NAME);
    }

    @Override
    public void setup(Project project) {
        project.getPluginManager().withPlugin("java",
            plugin -> project.getExtensions().create(JarJarExtension.NAME, JarJarExtensionImpl.class)
        );
    }
}
