/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

import org.gradle.api.Action;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderConvertible;

public sealed interface JarJarMetadataInfo permits JarJarMetadataInfoInternal {
    JarJarMetadataInfoModule getModule();

    default void module(Action<? super JarJarMetadataInfoModule> module) {
        module.execute(this.getModule());
    }

    void setVersion(CharSequence version);

    default void setVersion(Provider<? extends CharSequence> version) {
        this.setVersion(version.get());
    }

    default void setVersion(ProviderConvertible<? extends CharSequence> version) {
        this.setVersion(version.asProvider());
    }

    void setRange(CharSequence version);

    default void setRange(Provider<? extends CharSequence> version) {
        this.setRange(version.get());
    }

    default void setRange(ProviderConvertible<? extends CharSequence> version) {
        this.setRange(version.asProvider());
    }
}
