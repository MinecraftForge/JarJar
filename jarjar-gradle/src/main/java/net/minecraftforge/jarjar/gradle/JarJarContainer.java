/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.component.AdhocComponentWithVariants;

public interface JarJarContainer {
    String getConfigurationName();

    NamedDomainObjectProvider<DependencyScopeConfiguration> getConfiguration();

    default String getConsumableConfigurationName() {
        return this.getConfigurationName() + "RuntimeElements";
    }

    String getSoftwareComponentName();

    AdhocComponentWithVariants getSoftwareComponent();
}
