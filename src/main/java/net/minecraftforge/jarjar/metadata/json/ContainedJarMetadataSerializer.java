package net.minecraftforge.jarjar.metadata.json;

import com.google.gson.*;
import net.minecraftforge.jarjar.metadata.ContainedJarIdentifier;
import net.minecraftforge.jarjar.metadata.ContainedJarMetadata;
import net.minecraftforge.jarjar.metadata.ContainedVersion;
import org.apache.maven.artifact.versioning.VersionRange;

import java.lang.reflect.Type;

public class ContainedJarMetadataSerializer implements JsonSerializer<ContainedJarMetadata>, JsonDeserializer<ContainedJarMetadata>
{
    @Override
    public ContainedJarMetadata deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException
    {
        if (!json.isJsonObject())
            throw new JsonParseException("Expected object");

        final JsonObject jsonObject = json.getAsJsonObject();
        final ContainedJarIdentifier containedJarIdentifier = context.deserialize(jsonObject.get("identifier"), ContainedJarIdentifier.class);
        final ContainedVersion version = context.deserialize(jsonObject.get("version"), ContainedVersion.class);
        final String path = jsonObject.get("path").getAsString();
        return new ContainedJarMetadata(containedJarIdentifier, version, path);
    }

    @Override
    public JsonElement serialize(final ContainedJarMetadata src, final Type typeOfSrc, final JsonSerializationContext context)
    {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.add("identifier", context.serialize(src.identifier()));
        jsonObject.add("version", context.serialize(src.version()));
        jsonObject.add("path", new JsonPrimitive(src.path()));
        return jsonObject;
    }
}
