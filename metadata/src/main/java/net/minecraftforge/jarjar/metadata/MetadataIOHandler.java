/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.metadata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.jarjar.metadata.json.ArtifactVersionSerializer;
import net.minecraftforge.jarjar.metadata.json.ContainedJarIdentifierSerializer;
import net.minecraftforge.jarjar.metadata.json.ContainedJarMetadataSerializer;
import net.minecraftforge.jarjar.metadata.json.ContainedVersionSerializer;
import net.minecraftforge.jarjar.metadata.json.MetadataSerializer;
import net.minecraftforge.jarjar.metadata.json.VersionRangeSerializer;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Optional;

public final class MetadataIOHandler {
    private MetadataIOHandler() { }

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
