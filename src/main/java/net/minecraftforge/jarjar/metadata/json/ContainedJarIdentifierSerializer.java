package net.minecraftforge.jarjar.metadata.json;

import com.google.gson.*;
import net.minecraftforge.jarjar.metadata.ContainedJarIdentifier;

import java.lang.reflect.Type;

public class ContainedJarIdentifierSerializer implements JsonSerializer<ContainedJarIdentifier>, JsonDeserializer<ContainedJarIdentifier>
{
    @Override
    public ContainedJarIdentifier deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException
    {
        if (!json.isJsonObject())
            throw new JsonParseException("Expected object");

        final JsonObject jsonObject = json.getAsJsonObject();
        final String group = jsonObject.get("group").getAsString();
        final String artifact = jsonObject.get("artifact").getAsString();
        return new ContainedJarIdentifier(group, artifact);
    }

    @Override
    public JsonElement serialize(final ContainedJarIdentifier src, final Type typeOfSrc, final JsonSerializationContext context)
    {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("group", src.group());
        jsonObject.addProperty("artifact", src.artifact());
        return jsonObject;
    }
}
