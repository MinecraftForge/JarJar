/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;

non-sealed interface JarJarExtensionInternal extends JarJarExtension, HasPublicType {
    @Override
    default TypeOf<?> getPublicType() {
        return TypeOf.typeOf(JarJarExtension.class);
    }

    JarJarContainer getContainer();

    @Override
    default String getConfigurationName() {
        return this.getContainer().getConfigurationName();
    }

    @Override
    default String getConsumableConfigurationName() {
        return this.getContainer().getConsumableConfigurationName();
    }

    @Override
    default String getSoftwareComponentName() {
        return this.getContainer().getSoftwareComponentName();
    }

    @Override
    default NamedDomainObjectProvider<AdhocComponentWithVariants> getSoftwareComponent() {
        return this.getContainer().getSoftwareComponent();
    }

    @Override
    default NamedDomainObjectProvider<DependencyScopeConfiguration> getConfiguration() {
        return this.getContainer().getConfiguration();
    }
}
