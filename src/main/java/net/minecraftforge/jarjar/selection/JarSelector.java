package net.minecraftforge.jarjar.selection;

import com.google.common.collect.*;
import net.minecraftforge.jarjar.metadata.*;
import net.minecraftforge.jarjar.util.Constants;
import org.apache.maven.artifact.versioning.VersionRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class JarSelector
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JarSelector.class);

    private JarSelector()
    {
        throw new IllegalStateException("Can not instantiate an instance of: JarSelector. This is a utility class");
    }

    public static <T, E extends Throwable> List<T> detectAndSelect(
      final List<T> source,
      final BiFunction<T, Path, Optional<InputStream>> resourceReader,
      final BiFunction<T, Path, Optional<T>> sourceProducer,
      final Function<T, String> identificationProducer,
      final Function<Multimap<ContainedJarIdentifier, SourceWithRequestedVersionRange<T>>, E> failureExceptionProducer
    ) throws E
    {
        final Set<DetectionResult<T>> detectedMetadata = detect(source, resourceReader, sourceProducer, identificationProducer);
        final Map<ContainedJarMetadata, T> detectedJarsBySource = detectedMetadata.stream().collect(Collectors.toMap(DetectionResult::metadata, DetectionResult::source));
        final Map<ContainedJarMetadata, T> detectedJarsByRootSource = detectedMetadata.stream().collect(Collectors.toMap(DetectionResult::metadata, DetectionResult::rootSource));
        final Multimap<ContainedJarIdentifier, ContainedJarMetadata> metadataByIdentifier = Multimaps.index(detectedJarsByRootSource.keySet(), ContainedJarMetadata::identifier);

        final Set<SelectionResult> select = select(detectedJarsBySource.keySet());

        if (select.stream().anyMatch(result -> !result.selected().isPresent()))
        {
            //We have entered into failure territory. Let's collect all of those that failed
            final Set<SelectionResult> failed = select.stream().filter(result -> !result.selected().isPresent()).collect(Collectors.toSet());
            final Set<ContainedJarIdentifier> failedIdentifiers = failed.stream().map(SelectionResult::identifier).collect(Collectors.toSet());

            final Multimap<ContainedJarIdentifier, SourceWithRequestedVersionRange<T>> failedSources = HashMultimap.create();
            for (final ContainedJarIdentifier failedIdentifier : failedIdentifiers)
            {
                final Collection<ContainedJarMetadata> metadata = metadataByIdentifier.get(failedIdentifier);
                final Set<SourceWithRequestedVersionRange<T>> sources = metadata.stream().map(containedJarMetadata -> {
                    final T rootSource = detectedJarsByRootSource.get(containedJarMetadata);
                    return new SourceWithRequestedVersionRange<T>(rootSource, containedJarMetadata.version().range());
                })
                                                                                           .collect(Collectors.toSet());

                failedSources.putAll(failedIdentifier, sources);
            }

            final E exception = failureExceptionProducer.apply(failedSources);
            LOGGER.error("Failed to select jars for {}", failedIdentifiers);
            throw exception;
        }

        return select.stream()
                 .map(SelectionResult::selected)
                 .filter(Optional::isPresent)
                 .map(Optional::get)
                 .filter(detectedJarsBySource::containsKey)
                 .map(selectedJarMetadata -> {
                     final T sourceOfJar = detectedJarsBySource.get(selectedJarMetadata);
                     return sourceProducer.apply(sourceOfJar, Paths.get(selectedJarMetadata.path()));
                 })
                 .filter(Optional::isPresent)
                 .map(Optional::get)
                 .collect(Collectors.toList());
    }

    private static <T> Set<DetectionResult<T>> detect(
      final List<T> source,
      final BiFunction<T, Path, Optional<InputStream>> resourceReader,
      final BiFunction<T, Path, Optional<T>> sourceProducer,
      final Function<T, String> identificationProducer)
    {
        final Path metadataPath = Paths.get(Constants.CONTAINED_JARS_METADATA_PATH);
        final Map<T, Optional<InputStream>> metadataInputStreamsBySource = source.stream().collect(
          Collectors.toMap(
            Function.identity(),
            t -> resourceReader.apply(t, metadataPath)
          )
        );


        final Map<T, Metadata> rootMetadataBySource = metadataInputStreamsBySource.entrySet().stream()
                                                        .filter(kvp -> kvp.getValue().isPresent())
                                                        .map(kvp -> new SourceWithOptionalMetadata<>(kvp.getKey(), MetadataIOHandler.fromStream(kvp.getValue().get())))
                                                        .filter(sourceWithOptionalMetadata -> sourceWithOptionalMetadata.metadata().isPresent())
                                                        .collect(
                                                          Collectors.toMap(
                                                            SourceWithOptionalMetadata::source,
                                                            sourceWithOptionalMetadata -> sourceWithOptionalMetadata.metadata().get()
                                                          )
                                                        );

        return recursivelyDetectContainedJars(
          rootMetadataBySource,
          resourceReader,
          sourceProducer,
          identificationProducer
        );
    }

    private static <T> Set<DetectionResult<T>> recursivelyDetectContainedJars(
      final Map<T, Metadata> rootMetadataBySource,
      final BiFunction<T, Path, Optional<InputStream>> resourceReader,
      final BiFunction<T, Path, Optional<T>> sourceProducer,
      final Function<T, String> identificationProducer)
    {
        //final Multimap<T, ContainedJarMetadata> containedJarMetadatasBySource = HashMultimap.create();
        final Set<DetectionResult<T>> results = Sets.newHashSet();
        final Map<T, T> rootSourcesBySource = Maps.newHashMap();

        final Queue<T> sourcesToProcess = new LinkedList<>();
        for (final Map.Entry<T, Metadata> entry : rootMetadataBySource.entrySet())
        {
            entry.getValue().jars().stream().map(containedJarMetadata -> new DetectionResult<>(containedJarMetadata, entry.getKey(), entry.getKey()))
              .forEach(results::add);

            for (final ContainedJarMetadata jar : entry.getValue().jars())
            {
                final Optional<T> source = sourceProducer.apply(entry.getKey(), Paths.get(jar.path()));
                if (source.isPresent())
                {
                    sourcesToProcess.add(source.get());
                    rootSourcesBySource.put(source.get(), entry.getKey());
                }
                else
                {
                    LOGGER.warn("The source jar: " + identificationProducer.apply(entry.getKey()) + " is supposed to contain a jar: " + jar.path() + " but it does not exist.");
                }
            }
        }

        while (!sourcesToProcess.isEmpty())
        {
            final T source = sourcesToProcess.remove();
            final T rootSource = rootSourcesBySource.get(source);
            final Optional<InputStream> metadataInputStream = resourceReader.apply(source, Paths.get(Constants.CONTAINED_JARS_METADATA_PATH));
            if (metadataInputStream.isPresent())
            {
                final Optional<Metadata> metadata = MetadataIOHandler.fromStream(metadataInputStream.get());
                if (metadata.isPresent())
                {
                    metadata.get().jars().stream().map(containedJarMetadata -> new DetectionResult<>(containedJarMetadata, source, rootSource))
                      .forEach(results::add);

                    for (final ContainedJarMetadata jar : metadata.get().jars())
                    {
                        final Optional<T> sourceJar = sourceProducer.apply(source, Paths.get(jar.path()));
                        if (sourceJar.isPresent())
                        {
                            sourcesToProcess.add(sourceJar.get());
                            rootSourcesBySource.put(sourceJar.get(), rootSource);
                        }
                        else
                        {
                            LOGGER.warn("The source jar: " + identificationProducer.apply(source) + " is supposed to contain a jar: " + jar.path() + " but it does not exist.");
                        }
                    }
                }
            }
        }

        return results;
    }

    private static Set<SelectionResult> select(final Set<ContainedJarMetadata> containedJarMetadata)
    {
        final Multimap<ContainedJarIdentifier, ContainedJarMetadata> jarsByIdentifier = containedJarMetadata.stream()
                                                                                          .collect(
                                                                                            Multimaps.toMultimap(
                                                                                              ContainedJarMetadata::identifier,
                                                                                              Function.identity(),
                                                                                              HashMultimap::create
                                                                                            )
                                                                                          );

        return jarsByIdentifier.keySet().stream()
                 .map(identifier -> {
                     final Collection<ContainedJarMetadata> jars = jarsByIdentifier.get(identifier);

                     if (jars.size() <= 1)
                     {
                         //Quick return:
                         return new SelectionResult(identifier, jars, Optional.of(jars.iterator().next()));
                     }

                     //Find the most agreeable version:
                     final VersionRange range = jars.stream()
                                                  .map(ContainedJarMetadata::version)
                                                  .map(ContainedVersion::range)
                                                  .reduce(null, JarSelector::restrictRanges);

                     if (range == null || !isValid(range))
                     {
                         return new SelectionResult(identifier, jars, Optional.empty());
                     }

                     if (range.getRecommendedVersion() != null)
                     {
                         final Optional<ContainedJarMetadata> selected =
                           jars.stream().filter(jar -> jar.version().artifactVersion().equals(range.getRecommendedVersion())).findFirst();
                         return new SelectionResult(identifier, jars, selected);
                     }

                     final Optional<ContainedJarMetadata> selected = jars.stream().filter(jar -> range.containsVersion(jar.version().artifactVersion())).findFirst();
                     return new SelectionResult(identifier, jars, selected);
                 })
                 .collect(Collectors.toSet());
    }

    private static VersionRange restrictRanges(final VersionRange versionRange, final VersionRange versionRange2)
    {
        if (versionRange == null)
        {
            return versionRange2;
        }

        if (versionRange2 == null)
        {
            return versionRange;
        }

        return versionRange.restrict(versionRange);
    }

    private static boolean isValid(final VersionRange range)
    {
        return range.getRecommendedVersion() == null && !range.hasRestrictions();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final class SourceWithOptionalMetadata<Z>
    {
        private final Z source;
        private final Optional<Metadata> metadata;

        SourceWithOptionalMetadata(Z source, Optional<Metadata> metadata)
        {
            this.source = source;
            this.metadata = metadata;
        }

        public Z source() {return source;}

        public Optional<Metadata> metadata() {return metadata;}

        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            final SourceWithOptionalMetadata that = (SourceWithOptionalMetadata) obj;
            return Objects.equals(this.source, that.source) &&
                     Objects.equals(this.metadata, that.metadata);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(source, metadata);
        }

        @Override
        public String toString()
        {
            return "SourceWithOptionalMetadata[" +
                     "source=" + source + ", " +
                     "metadata=" + metadata + ']';
        }
    }

    private static final class DetectionResult<Z>
    {
        private final ContainedJarMetadata metadata;
        private final Z source;
        private final Z rootSource;

        private DetectionResult(ContainedJarMetadata metadata, Z source, Z rootSource)
        {
            this.metadata = metadata;
            this.source = source;
            this.rootSource = rootSource;
        }

        public ContainedJarMetadata metadata() {return metadata;}

        public Z source() {return source;}

        public Z rootSource() {return rootSource;}

        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            final DetectionResult that = (DetectionResult) obj;
            return Objects.equals(this.metadata, that.metadata) &&
                     Objects.equals(this.source, that.source) &&
                     Objects.equals(this.rootSource, that.rootSource);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(metadata, source, rootSource);
        }

        @Override
        public String toString()
        {
            return "DetectionResult[" +
                     "metadata=" + metadata + ", " +
                     "source=" + source + ", " +
                     "rootSource=" + rootSource + ']';
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final class SelectionResult
    {
        private final ContainedJarIdentifier identifier;
        private final Collection<ContainedJarMetadata> candidates;
        private final Optional<ContainedJarMetadata>   selected;

        private SelectionResult(ContainedJarIdentifier identifier, Collection<ContainedJarMetadata> candidates, Optional<ContainedJarMetadata> selected)
        {
            this.identifier = identifier;
            this.candidates = candidates;
            this.selected = selected;
        }

        public ContainedJarIdentifier identifier() {return identifier;}

        public Collection<ContainedJarMetadata> candidates() {return candidates;}

        public Optional<ContainedJarMetadata> selected() {return selected;}

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            final SelectionResult that = (SelectionResult) obj;
            return Objects.equals(this.identifier, that.identifier) &&
                     Objects.equals(this.candidates, that.candidates) &&
                     Objects.equals(this.selected, that.selected);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(identifier, candidates, selected);
        }

        @Override
        public String toString()
        {
            return "SelectionResult[" +
                     "identifier=" + identifier + ", " +
                     "candidates=" + candidates + ", " +
                     "selected=" + selected + ']';
        }
    }

    public static final class SourceWithRequestedVersionRange<Z>
    {
        private final Z source;
        private final VersionRange requestedVersionRange;

        public SourceWithRequestedVersionRange(Z source, VersionRange requestedVersionRange)
        {
            this.source = source;
            this.requestedVersionRange = requestedVersionRange;
        }

        public Z source() {return source;}

        public VersionRange requestedVersionRange() {return requestedVersionRange;}

        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            final SourceWithRequestedVersionRange that = (SourceWithRequestedVersionRange) obj;
            return Objects.equals(this.source, that.source) &&
                     Objects.equals(this.requestedVersionRange, that.requestedVersionRange);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(source, requestedVersionRange);
        }

        @Override
        public String toString()
        {
            return "SourceWithRequestedVersionRange[" +
                     "source=" + source + ", " +
                     "requestedVersionRange=" + requestedVersionRange + ']';
        }
    }
}
