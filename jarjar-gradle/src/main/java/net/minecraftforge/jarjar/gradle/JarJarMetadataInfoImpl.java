/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Objects;

abstract class JarJarMetadataInfoImpl implements JarJarMetadataInfoInternal {
    private final Dependency dependency;
    private final ExtraPropertiesExtension ext;

    @Inject
    public JarJarMetadataInfoImpl(Dependency dependency) {
        this.dependency = dependency;
        this.ext = ((ExtensionAware) dependency).getExtensions().getExtraProperties();
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    public @Nullable String getGroup() {
        return ext.has(MODULE_GROUP_PROPERTY) ? ext.get(MODULE_GROUP_PROPERTY).toString() : dependency.getGroup();
    }

    @Override
    public void setGroup(CharSequence group) {
        this.ext.set(MODULE_GROUP_PROPERTY, group);
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    public String getName() {
        return ext.has(MODULE_NAME_PROPERTY) ? ext.get(MODULE_NAME_PROPERTY).toString() : dependency.getName();
    }

    @Override
    public void setName(CharSequence name) {
        this.ext.set(MODULE_NAME_PROPERTY, name);
    }


    /* INTERNAL IMPL */

    @Override
    public @Nullable String getVersion() {
        try {
            return Objects.requireNonNull(ext.get(VERSION_PROPERTY)).toString();
        } catch (ExtraPropertiesExtension.UnknownPropertyException | NullPointerException e) {
            return this.dependency.getVersion();
        }
    }

    @Override
    public void setVersion(CharSequence version) {
        this.ext.set(VERSION_PROPERTY, version);
    }

    @Override
    public @Nullable String getRange() {
        try {
            return Objects.requireNonNull(ext.get(VERSION_RANGE_PROPERTY)).toString();
        } catch (ExtraPropertiesExtension.UnknownPropertyException | NullPointerException e) {
            return this.getVersion();
        }
    }

    @Override
    public void setRange(CharSequence range) {
        this.ext.set(VERSION_RANGE_PROPERTY, range);
    }

    public boolean hasManuallySpecifiedRange() {
        return this.ext.has(VERSION_RANGE_PROPERTY) && this.ext.get(VERSION_RANGE_PROPERTY) != null;
    }
}
