/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.metadata.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.minecraftforge.jarjar.metadata.ContainedVersion;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;

import java.lang.reflect.Type;

public class ContainedVersionSerializer implements JsonSerializer<ContainedVersion>, JsonDeserializer<ContainedVersion> {
    @Override
    public ContainedVersion deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonObject())
            throw new JsonParseException("Expected object");

        final JsonObject jsonObject = json.getAsJsonObject();
        final VersionRange range = context.deserialize(jsonObject.get("range"), VersionRange.class);
        final ArtifactVersion artifactVersion = context.deserialize(jsonObject.get("artifactVersion"), ArtifactVersion.class);
        return new ContainedVersion(range, artifactVersion);
    }

    @Override
    public JsonElement serialize(final ContainedVersion src, final Type typeOfSrc, final JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.add("range", context.serialize(src.range()));
        jsonObject.add("artifactVersion", context.serialize(src.artifactVersion()));
        return jsonObject;
    }
}
