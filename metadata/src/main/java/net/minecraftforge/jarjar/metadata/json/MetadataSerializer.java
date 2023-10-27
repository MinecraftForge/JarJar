/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.metadata.json;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.jarjar.metadata.ContainedJarMetadata;
import net.minecraftforge.jarjar.metadata.Metadata;

import java.lang.reflect.Type;
import java.util.List;

public class MetadataSerializer implements JsonSerializer<Metadata>, JsonDeserializer<Metadata>
{
    private static final TypeToken<List<ContainedJarMetadata>> LIST_TOKEN = new TypeToken<List<ContainedJarMetadata>>() {};

    @Override
    public Metadata deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException
    {
        if (!json.isJsonObject())
            throw new JsonParseException("Expected object");

        final JsonObject jsonObject = json.getAsJsonObject();
        final List<ContainedJarMetadata> jars = context.deserialize(jsonObject.get("jars"), LIST_TOKEN.getType());
        return new Metadata(jars);
    }

    @Override
    public JsonElement serialize(final Metadata src, final Type typeOfSrc, final JsonSerializationContext context)
    {
        final JsonObject json = new JsonObject();
        json.add("jars", context.serialize(src.jars()));
        return json;
    }
}
