package net.minecraftforge.jarjar.metadata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.jarjar.metadata.json.ArtifactVersionSerializer;
import net.minecraftforge.jarjar.metadata.json.VersionRangeSerializer;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;

public class MetadataIOHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataIOHandler.class);
    private static final Gson GSON = new GsonBuilder()
                                       .registerTypeAdapter(VersionRange.class, new VersionRangeSerializer())
                                       .registerTypeAdapter(ArtifactVersion.class, new ArtifactVersionSerializer())
                                       .setPrettyPrinting()
                                       .create();

    private MetadataIOHandler()
    {
        throw new IllegalStateException("Can not instantiate an instance of: MetadataIOHandler. This is a utility class");
    }

    public static Optional<Metadata> fromStream(final InputStream stream) {
        try {
            return Optional.of(GSON.fromJson(new InputStreamReader(stream), Metadata.class));
        } catch (Exception e) {
            LOGGER.error("Failed to parse metadata", e);
            return Optional.empty();
        }
    }
}
