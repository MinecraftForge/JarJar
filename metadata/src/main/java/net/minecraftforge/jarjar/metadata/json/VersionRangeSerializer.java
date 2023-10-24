/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.metadata.json;

import com.google.gson.*;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.Restriction;
import org.apache.maven.artifact.versioning.VersionRange;

import java.lang.reflect.Type;
import java.util.Iterator;

public class VersionRangeSerializer implements JsonSerializer<VersionRange>, JsonDeserializer<VersionRange>
{
    @Override
    public VersionRange deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException
    {
        if (json.isJsonPrimitive()) {
            try
            {
                return VersionRange.createFromVersionSpec(json.getAsString());
            }
            catch (InvalidVersionSpecificationException e)
            {
                throw new JsonParseException("Failed to parse version spec from: " + json.getAsString(), e);
            }
        }

        throw new JsonParseException("Expected a string or primitive value");
    }

    @Override
    public JsonElement serialize(final VersionRange src, final Type typeOfSrc, final JsonSerializationContext context)
    {
        return new JsonPrimitive(serializeRange(src));
    }

    private String serializeRange(final VersionRange src)
    {
        if ( src.getRecommendedVersion() != null )
        {
            return src.getRecommendedVersion().toString();
        }
        else
        {
            StringBuilder buf = new StringBuilder();
            for (Iterator<Restriction> i = src.getRestrictions().iterator(); i.hasNext(); )
            {
                Restriction r = i.next();

                buf.append( serializeRestriction(r) );

                if ( i.hasNext() )
                {
                    buf.append( ',' );
                }
            }
            return buf.toString();
        }
    }

    private String serializeRestriction(final Restriction src)
    {
        StringBuilder buf = new StringBuilder();

        buf.append( src.isLowerBoundInclusive() ? '[' : '(' );
        if (src.getLowerBound().equals(src.getUpperBound())) {
            buf.append(src.getLowerBound().toString());
        } else {
            if ( src.getLowerBound() != null )
            {
                buf.append( src.getLowerBound().toString() );
            }
            buf.append( ',' );
            if ( src.getUpperBound() != null )
            {
                buf.append( src.getUpperBound().toString() );
            }
        }

        buf.append( src.isUpperBoundInclusive() ? ']' : ')' );

        return buf.toString();
    }
}
