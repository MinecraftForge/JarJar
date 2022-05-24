package net.minecraftforge.jarjar.metadata.json;

import com.google.gson.*;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

import java.lang.reflect.Type;

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
        return new JsonPrimitive(src.toString());
    }
}
