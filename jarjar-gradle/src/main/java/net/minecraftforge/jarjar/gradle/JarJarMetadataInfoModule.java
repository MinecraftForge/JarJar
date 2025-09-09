/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderConvertible;
import org.jetbrains.annotations.Nullable;

public sealed interface JarJarMetadataInfoModule extends ModuleIdentifier permits JarJarMetadataInfoInternal {
    @Override
    @Nullable String getGroup();

    void setGroup(CharSequence group);

    default void setGroup(Provider<? extends CharSequence> group) {
        this.setGroup(group.get());
    }

    default void setGroup(ProviderConvertible<? extends CharSequence> group) {
        this.setGroup(group.asProvider());
    }

    @Override
    String getName();

    void setName(CharSequence name);

    default void setName(Provider<? extends CharSequence> name) {
        this.setName(name.get());
    }

    default void setName(ProviderConvertible<? extends CharSequence> name) {
        this.setName(name.asProvider());
    }
}
