/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle;

import net.minecraftforge.gradleutils.shared.Tool;

final class Tools {
    private Tools() { }

    // TODO [JarJar-Gradle] Use this once the shadow jar is built
    static final Tool JARJAR_LEGACY_METADATA = Tool.of(
        Constants.JARJAR_LEGACY_METADATA_NAME,
        Constants.JARJAR_LEGACY_VERSION,
        Constants.JARJAR_LEGACY_METADATA_DOWNLOAD_URL,
        Constants.JARJAR_LEGACY_MIN_JAVA
        // no main class
    );
}
