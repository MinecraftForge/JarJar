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
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.minecraftforge.jarjar.metadata.ContainedJarIdentifier;
import net.minecraftforge.jarjar.metadata.ContainedJarMetadata;
import net.minecraftforge.jarjar.metadata.ContainedVersion;

import java.lang.reflect.Type;

public class ContainedJarMetadataSerializer implements JsonSerializer<ContainedJarMetadata>, JsonDeserializer<ContainedJarMetadata> {
    @Override
    public ContainedJarMetadata deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonObject())
            throw new JsonParseException("Expected object");

        final JsonObject jsonObject = json.getAsJsonObject();
        final ContainedJarIdentifier containedJarIdentifier = context.deserialize(jsonObject.get("identifier"), ContainedJarIdentifier.class);
        final ContainedVersion version = context.deserialize(jsonObject.get("version"), ContainedVersion.class);
        final String path = jsonObject.get("path").getAsString();
        boolean isObfuscated = false;
        if (jsonObject.has("isObfuscated"))
            isObfuscated = jsonObject.get("isObfuscated").getAsBoolean();

        return new ContainedJarMetadata(containedJarIdentifier, version, path, isObfuscated);
    }

    @Override
    public JsonElement serialize(final ContainedJarMetadata src, final Type typeOfSrc, final JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.add("identifier", context.serialize(src.identifier()));
        jsonObject.add("version", context.serialize(src.version()));
        jsonObject.add("path", new JsonPrimitive(src.path()));
        jsonObject.add("isObfuscated", new JsonPrimitive(src.isObfuscated()));
        return jsonObject;
    }
}
