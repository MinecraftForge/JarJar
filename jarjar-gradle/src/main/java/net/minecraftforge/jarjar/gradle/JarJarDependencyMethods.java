/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;
import net.minecraftforge.gradleutils.shared.Closures;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.ExtensionAware;

import java.util.Objects;

public class JarJarDependencyMethods {
    public static void jarJar(
        Dependency self,
        @DelegatesTo(JarJarMetadataInfo.class)
        @ClosureParams(value = SimpleType.class, options = "net.minecraftforge.jarjar.gradle.JarJarDependency")
        Closure<?> closure
    ) {
        jarJar(self, Closures.toAction(closure));
    }

    public static void jarJar(Dependency self, Action<? super JarJarMetadataInfo> action) {
        action.execute(getJarJar(self));
    }

    public static JarJarMetadataInfo getJarJar(Dependency self) {
        var extensions = ((ExtensionAware) self).getExtensions();
        return Objects.requireNonNullElseGet(
            extensions.findByType(JarJarMetadataInfo.class),
            () -> extensions.create(JarJarExtension.NAME, JarJarMetadataInfoImpl.class, self)
        );
    }
}
