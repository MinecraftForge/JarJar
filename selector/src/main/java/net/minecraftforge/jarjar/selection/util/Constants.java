/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.selection.util;

public class Constants
{

    private Constants()
    {
        throw new IllegalStateException("Can not instantiate an instance of: Constants. This is a utility class");
    }

    public static final String CONTAINED_JARS_METADATA_PATH = "META-INF/jarjar/metadata.json";
}
