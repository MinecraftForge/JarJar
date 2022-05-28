package net.minecraftforge.jarjar.selection;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import net.minecraftforge.jarjar.metadata.ContainedJarIdentifier;
import net.minecraftforge.jarjar.metadata.ContainedJarMetadata;
import net.minecraftforge.jarjar.metadata.Metadata;
import net.minecraftforge.jarjar.metadata.MetadataIOHandler;
import net.minecraftforge.jarjar.util.Constants;
import org.apache.maven.artifact.versioning.VersionRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class JarSelector
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JarSelector.class);

    private JarSelector()
    {
        throw new IllegalStateException("Can not instantiate an instance of: JarSelector. This is a utility class");
    }

    public static <T> List<T> detectAndSelect(
      final List<T> source,
      final BiFunction<T, Path, Optional<InputStream>> resourceReader,
      final BiFunction<T, Path, Optional<T>> sourceProducer,
      final Function<T, String> identificationProducer
    ) {
        final Map<ContainedJarMetadata, T> detectedJarsBySource = detect(source, resourceReader, sourceProducer, identificationProducer);
        final Set<ContainedJarMetadata> select = select(detectedJarsBySource.keySet());
        return select.stream().filter(detectedJarsBySource::containsKey).map(selectedJarMetadata -> {
            final T sourceOfJar = detectedJarsBySource.get(selectedJarMetadata);
            return sourceProducer.apply(sourceOfJar, Path.of(selectedJarMetadata.path()));
        })
                 .filter(Optional::isPresent)
                 .map(Optional::get)
                 .collect(Collectors.toList());
    }

    private static <T> Map<ContainedJarMetadata, T> detect(
      final List<T> source,
      final BiFunction<T, Path, Optional<InputStream>> resourceReader,
      final BiFunction<T, Path, Optional<T>> sourceProducer,
      final Function<T, String> identificationProducer)
    {
        final Path metadataPath = Path.of(Constants.CONTAINED_JARS_METADATA_PATH);
        final Map<T, Optional<InputStream>> metadataInputStreamsBySource = source.stream().collect(
          Collectors.toMap(
            Function.identity(),
            t -> resourceReader.apply(t, metadataPath)
          )
        );

        record SourceWithOptionalMetadata<Z>(Z source, Optional<Metadata> metadata) {}
        final Map<T, Metadata> metadataBySource = metadataInputStreamsBySource.entrySet().stream()
                                                    .filter(kvp -> kvp.getValue().isPresent())
                                                    .map(kvp -> new SourceWithOptionalMetadata<>(kvp.getKey(), MetadataIOHandler.fromStream(kvp.getValue().get())))
                                                    .filter(sourceWithOptionalMetadata -> sourceWithOptionalMetadata.metadata().isPresent())
                                                    .collect(
                                                      Collectors.toMap(
                                                        SourceWithOptionalMetadata::source,
                                                        sourceWithOptionalMetadata -> sourceWithOptionalMetadata.metadata().get()
                                                      )
                                                    );

        final Multimap<T, ContainedJarMetadata> containedJarMetadatasBySource = recursivelyDetectContainedJars(metadataBySource,
          resourceReader,
          sourceProducer,
          identificationProducer);
        return containedJarMetadatasBySource.entries().stream()
                                                                             .collect(
                                                                               Collectors.toMap(
                                                                                 Map.Entry::getValue,
                                                                                 Map.Entry::getKey
                                                                               )
                                                                             );
    }

    private static <T> Multimap<T, ContainedJarMetadata> recursivelyDetectContainedJars(
      final Map<T, Metadata> metadataBySource,
      final BiFunction<T, Path, Optional<InputStream>> resourceReader,
      final BiFunction<T, Path, Optional<T>> sourceProducer,
      final Function<T, String> identificationProducer)
    {
        final Multimap<T, ContainedJarMetadata> containedJarMetadatasBySource = HashMultimap.create();
        final Queue<T> sourcesToProcess = new LinkedList<>();
        for (final Map.Entry<T, Metadata> entry : metadataBySource.entrySet())
        {
            containedJarMetadatasBySource.putAll(entry.getKey(), entry.getValue().jars());
            for (final ContainedJarMetadata jar : entry.getValue().jars())
            {
                final Optional<T> source = sourceProducer.apply(entry.getKey(), Path.of(jar.path()));
                if (source.isPresent()) {
                    sourcesToProcess.add(source.get());
                }
                else
                {
                    LOGGER.warn("The source jar: " + identificationProducer.apply(entry.getKey()) + " is supposed to contain a jar: " + jar.path() + " but it does not exist.");
                }
            }
        }

        while(!sourcesToProcess.isEmpty()) {
            final T source = sourcesToProcess.remove();
            final Optional<InputStream> metadataInputStream = resourceReader.apply(source, Path.of(Constants.CONTAINED_JARS_METADATA_PATH));
            if (metadataInputStream.isPresent()) {
                final Optional<Metadata> metadata = MetadataIOHandler.fromStream(metadataInputStream.get());
                if (metadata.isPresent()) {
                    containedJarMetadatasBySource.putAll(source, metadata.get().jars());
                    for (final ContainedJarMetadata jar : metadata.get().jars()) {
                        final Optional<T> sourceJar = sourceProducer.apply(source, Path.of(jar.path()));
                        if (sourceJar.isPresent()) {
                            sourcesToProcess.add(sourceJar.get());
                        }
                        else
                        {
                            LOGGER.warn("The source jar: " + identificationProducer.apply(source) + " is supposed to contain a jar: " + jar.path() + " but it does not exist.");
                        }
                    }
                }
            }
            else
            {
                LOGGER.warn("The source jar: " + identificationProducer.apply(source) + " is supposed to contain a contained jars metadata but it does not exist.");
            }
        }

        return containedJarMetadatasBySource;
    }

    private static Set<ContainedJarMetadata> select(final Set<ContainedJarMetadata> containedJarMetadata) {
        final Multimap<ContainedJarIdentifier, ContainedJarMetadata> jarsByIdentifier = containedJarMetadata.stream()
                                                                                   .collect(
                                                                                     Multimaps.toMultimap(
                                                                                       ContainedJarMetadata::identifier,
                                                                                       Function.identity(),
                                                                                       HashMultimap::create
                                                                                     )
                                                                                   );
        return jarsByIdentifier.keySet().stream()
                 .map(jarsByIdentifier::get)
                 .flatMap(jars -> {
                     if (jars.size() <= 1) {
                         //Quick return:
                         return jars.stream();
                     }

                     //Find the most agreeable version:
                     final VersionRange range = jars.stream()
                                                  .map(ContainedJarMetadata::version)
                                                  .reduce(null, JarSelector::restrictRanges);

                     if (range == null) {
                         return Stream.empty();
                     }

                     if (!isValid(range)) {
                         return Stream.empty();
                     }

                     if (range.getRecommendedVersion() != null) {
                         return jars.stream().filter(jar -> jar.version().getRecommendedVersion().equals(range.getRecommendedVersion())).findFirst().stream();
                     }

                     return jars.stream().filter(jar -> range.containsVersion(jar.version().getRecommendedVersion())).findFirst().stream();
                 })
                 .collect(Collectors.toSet());
    }


    private static VersionRange restrictRanges(final VersionRange versionRange, final VersionRange versionRange2)
    {
        if (versionRange == null) {
            return versionRange2;
        }

        if (versionRange2 == null) {
            return versionRange;
        }

        return versionRange.restrict(versionRange);
    }

    private static boolean isValid(final VersionRange range) {
        return range.getRecommendedVersion() == null && !range.hasRestrictions();
    }
}
