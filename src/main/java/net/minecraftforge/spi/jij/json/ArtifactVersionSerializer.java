package net.minecraftforge.spi.jij.json;

import com.google.gson.*;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.lang.reflect.Type;

public class ArtifactVersionSerializer implements JsonSerializer<ArtifactVersion>, JsonDeserializer<ArtifactVersion>
{
    @Override
    public ArtifactVersion deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException
    {
        if (!json.isJsonPrimitive()) {
            throw new JsonParseException("Expected a string, but got: " + json);
        }

        return new DefaultArtifactVersion(json.getAsString());
    }

    @Override
    public JsonElement serialize(final ArtifactVersion src, final Type typeOfSrc, final JsonSerializationContext context)
    {
        return new JsonPrimitive(src.toString());
    }
}
