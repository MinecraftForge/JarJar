/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.metadata.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.Restriction;
import org.apache.maven.artifact.versioning.VersionRange;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.stream.Collectors;

public class VersionRangeSerializer implements JsonSerializer<VersionRange>, JsonDeserializer<VersionRange> {
    @Override
    public VersionRange deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonPrimitive()) {
            try {
                return VersionRange.createFromVersionSpec(json.getAsString());
            } catch (InvalidVersionSpecificationException e) {
                throw new JsonParseException("Failed to parse version spec from: " + json.getAsString(), e);
            }
        }

        throw new JsonParseException("Expected a string or primitive value");
    }

    @Override
    public JsonElement serialize(final VersionRange src, final Type typeOfSrc, final JsonSerializationContext context) {
        return new JsonPrimitive(serializeRange(src));
    }

    private static String serializeRange(final VersionRange src) {
        return src.getRecommendedVersion() != null
            ? src.getRecommendedVersion().toString()
            : src.getRestrictions()
                 .stream()
                 .map(VersionRangeSerializer::serializeRestriction)
                 .collect(Collectors.joining(","));
    }

    // NOTE: Does nothing but condense the version if lower and upper bounds are the same.
    private static String serializeRestriction(final Restriction src) {
        return src.getLowerBound().equals(src.getUpperBound())
            ? String.format("%s%s%s", src.isLowerBoundInclusive() ? '[' : '(', src.getLowerBound().toString(), src.isUpperBoundInclusive() ? ']' : ')')
            : src.toString();
    }
}
