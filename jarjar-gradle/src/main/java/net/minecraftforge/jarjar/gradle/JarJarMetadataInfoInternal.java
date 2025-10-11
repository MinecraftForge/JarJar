/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.jetbrains.annotations.Nullable;

non-sealed interface JarJarMetadataInfoInternal extends JarJarMetadataInfo, JarJarMetadataInfoModule, HasPublicType {
    String MODULE_GROUP_PROPERTY = "__jarJar_module_group";
    String MODULE_NAME_PROPERTY = "__jarJar_module_name";
    String VERSION_PROPERTY = "__jarJar_version";
    String VERSION_RANGE_PROPERTY = "__jarJar_version_range";
    String CONSTRAINT_PROPERTY = "__jarJar_constraint";

    @Override
    default TypeOf<?> getPublicType() {
        return TypeOf.typeOf(JarJarMetadataInfo.class);
    }

    @Override
    default JarJarMetadataInfoModule getModule() {
        return this;
    }

    @Nullable String getVersion();

    @Nullable String getRange();

    boolean isConstraint();

    boolean hasManuallySpecifiedRange();
}
