/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.metadata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.jarjar.metadata.json.*;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.codehaus.plexus.util.StringInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Optional;

public final class MetadataIOHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataIOHandler.class);
    private static final Gson GSON = new GsonBuilder()
                                       .registerTypeAdapter(VersionRange.class, new VersionRangeSerializer())
                                       .registerTypeAdapter(ArtifactVersion.class, new ArtifactVersionSerializer())
                                       .registerTypeAdapter(DefaultArtifactVersion.class, new ArtifactVersionSerializer())
                                       .registerTypeAdapter(ContainedJarIdentifier.class, new ContainedJarIdentifierSerializer())
                                       .registerTypeAdapter(ContainedJarMetadata.class, new ContainedJarMetadataSerializer())
                                       .registerTypeAdapter(ContainedVersion.class, new ContainedVersionSerializer())
                                       .registerTypeAdapter(Metadata.class, new MetadataSerializer())
                                       .setPrettyPrinting()
                                       .create();

    private MetadataIOHandler()
    {
        throw new IllegalStateException("Can not instantiate an instance of: MetadataIOHandler. This is a utility class");
    }

    public static Optional<Metadata> fromStream(final InputStream stream) {
        try {
            return Optional.of(GSON.fromJson(new InputStreamReader(stream), Metadata.class));
        } catch (Exception e) {
            LOGGER.error("Failed to parse metadata", e);
            return Optional.empty();
        }
    }

    public static Iterable<String> toLines(final Metadata metadata) {
        return Arrays.asList(GSON.toJson(metadata).split("\n"));
    }

    public static InputStream toInputStream(final Metadata metadata) {
        final String values = GSON.toJson(metadata);
        return new ByteArrayInputStream(values.getBytes());
    }
}
