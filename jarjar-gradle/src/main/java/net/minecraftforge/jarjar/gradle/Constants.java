/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

final class Constants {
    private Constants() { }

    // JarJar (Legacy) Metadata
    static final String JARJAR_LEGACY_METADATA_NAME = "JarJarMetadata";
    static final String JARJAR_LEGACY_VERSION = "0.3.33";
    static final String JARJAR_LEGACY_METADATA_DOWNLOAD_URL = "https://maven.minecraftforge.net/net/minecraftforge/JarJarMetadata/" + JARJAR_LEGACY_VERSION + "/JarJarMetadata-" + JARJAR_LEGACY_VERSION + "-fatjar.jar";
    static final int JARJAR_LEGACY_MIN_JAVA = 8;
}
