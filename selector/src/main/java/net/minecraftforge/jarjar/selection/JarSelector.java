/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.selection;

import net.minecraftforge.jarjar.metadata.*;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class JarSelector<T> {
    protected JarSelector() { }

    private static final Logger LOGGER = LoggerFactory.getLogger(JarSelector.class);

    public static final String CONTAINED_JARS_METADATA_PATH = "META-INF/jarjar/metadata.json";

    @Deprecated //(forRemoval = true)
    public static <T, E extends Throwable> List<T> detectAndSelect(
        final List<T> source,
        final BiFunction<T, Path, Optional<InputStream>> resourceReader,
        final BiFunction<T, Path, Optional<T>> sourceProducer,
        final Function<T, String> identificationProducer,
        final Function<Collection<ResolutionFailureInformation<T>>, E> failureExceptionProducer
    ) throws E {
        JarSelector<T> selector = new JarSelector<T>() {
            @Override
            @Nullable
            protected InputStream getResource(T source, String path) {
                return resourceReader.apply(source, Paths.get(path)).orElse(null);
            }

            @Override
            @Nullable
            protected T getNested(T source, String path) {
                return sourceProducer.apply(source, Paths.get(path)).orElse(null);
            }

            @Override
            protected String getIdentifier(T source) {
                return identificationProducer.apply(source);
            }

            @Override
            protected Throwable getFailureException(Collection<ResolutionFailureInformation<T>> failures) {
                return failureExceptionProducer.apply(failures);
            }
        };

        selector.force(source);
        return selector.select();
    }

    private final Set<T> seen = new HashSet<>();
    private final Set<DetectionResult<T>> detected = new HashSet<>();
    // Identifiers claimed by the source files, these will override any nested resolutions
    private final Map<String, T> claimed = new HashMap<>();
    private final Set<ContainedJarIdentifier> identifiers = new HashSet<>();

    /**
     * Returns if a dependency has been requested by something added by {@link #add(T)} or {@link #addRequirement(ContainedJarMetadata)}
     */
    public boolean isRequired(String group, String artifact) {
        return isRequired(new ContainedJarIdentifier(group, artifact));
    }

    /**
     * Returns if a dependency has been requested by something added by {@link #add(T)} or {@link #addRequirement(ContainedJarMetadata)}
     */
    public boolean isRequired(ContainedJarIdentifier identifier) {
        return identifiers.contains(identifier);
    }

    /**
     * Return a input stream for the given source and path.
     */
    protected abstract InputStream getResource(T source, String path);

    /**
     * Return a nested source, that we can then call getResource on to return the metadata
     */
    protected abstract T getNested(T source, String path);

    /**
     * Return a nice identification string for the given source, useful for logging
     * This should ideally match the identifier inside the Jar-in-Jar metadata.
     */
    protected abstract String getIdentifier(T source);

    /**
     * Create an exception for the given failures, it will be immediately thrown after calling
     */
    protected abstract Throwable getFailureException(Collection<ResolutionFailureInformation<T>> failures);

    /**
     * Force a version as a 'root' value.
     * This causes them to override any nested dependency with the same identifier.
     *
     * Also calls {@link #add(T)} on the source.
     */
    public void force(T source) {
        String id = getIdentifier(source);
        T old = claimed.putIfAbsent(id, source);
        if (old != null)
            LOGGER.warn("Attempted to force two jars which have the same identification {}: {} and {}. Using {}", id, old, source, old);

        add(source);
    }

    /**
     * Force the passed in sources the 'root' values.
     * This causes them to override any nested dependency with the same identifier.
     *
     * Also calls {@link #add(Collection<T>)} on the sources.
     */
    public void force(Collection<T> sources) {
        for (T source : sources) {
            String id = getIdentifier(source);
            T old = claimed.putIfAbsent(id, source);
            if (old != null)
                LOGGER.warn("Attempted to force two jars which have the same identification {}: {} and {}. Using {}", id, old, source, old);
        }

        add(sources);
    }

    /**
     * Adds a potential library to the options used when resolving dependencies.
     * This is useful for adding non-nested jars as options to satisfy dependencies.
     *
     * This does NOT call {@link #add(T)}, so if you expect this source to have nested jars, call it yourself
     */
    public void option(T source, ContainedJarMetadata meta) {
        this.detected.add(new DetectionResult<>(meta, source, 0));
    }

    /**
     * Adds a version restriction used when resolving dependencies.
     * This is meant to be used in conjuction with {@link #option(T,ContainedJarMetadata)} to add transitive dependencies.
     */
    public void addRequirement(ContainedJarMetadata meta) {
        this.detected.add(new DetectionResult<>(meta, null, 0));
    }

    /**
     * Recursively scans a source for jar-in-jar libraries
     */
    public void add(T source) {
        add(Arrays.asList(source));
    }

    /**
     * Recursively scans a collection of sources source for jar-in-jar libraries
     */
    public void add(Collection<T> source) {
        Map<T, Integer> depths = new HashMap<>();

        Queue<T> queue = new ArrayDeque<>(source);
        while (!queue.isEmpty()) {
            T current = queue.remove();

            // We've already seen this, skip re-procesing
            if (!seen.add(current))
                continue;

            InputStream is = getResource(current, CONTAINED_JARS_METADATA_PATH);
            if (is == null)
                continue;

            Metadata metadata = MetadataIOHandler.fromStream(is).orElse(null);
            if (metadata == null)
                continue;

            int depth = depths.getOrDefault(current, 0) + 1;

            for (ContainedJarMetadata jar : metadata.jars()) {
                T nested = jar.path() == null || jar.path().isEmpty() ? null : getNested(current, jar.path());

                DetectionResult<T> detection = new DetectionResult<>(jar, nested, depth);
                this.detected.add(detection);
                this.identifiers.add(jar.identifier());

                if (nested != null) {
                    queue.add(nested);
                    depths.put(nested, depth);
                }
            }
        }
    }

    public List<T> select() {
        Map<ContainedJarMetadata, Collection<DetectionResult<T>>> detectedJarsBySource = new HashMap<>();
        Map<ContainedJarIdentifier, Collection<ContainedJarMetadata>> metadataByIdentifier = new HashMap<>();
        Map<ContainedJarIdentifier, Collection<ContainedJarMetadata>> extraRestrictions = new HashMap<>();

        for (DetectionResult<T> detection : detected) {
            detectedJarsBySource.computeIfAbsent(detection.metadata, k -> new HashSet<>()).add(detection);
            Collection<ContainedJarMetadata> candidates = metadataByIdentifier.computeIfAbsent(detection.metadata.identifier(), k -> new HashSet<>());
            if (detection.source() == null)
                extraRestrictions.computeIfAbsent(detection.metadata.identifier(), k -> new HashSet<>()).add(detection.metadata);
            else
                candidates.add(detection.metadata);
        }

        Set<SelectionResult> options = new HashSet<>();
        boolean failed = false;

        for (Entry<ContainedJarIdentifier, Collection<ContainedJarMetadata>> entry : metadataByIdentifier.entrySet()) {
            ContainedJarIdentifier identifier = entry.getKey();
            Collection<ContainedJarMetadata> jars = entry.getValue();

            //Find the most agreeable version:
            VersionRange range = null;
            for (ContainedJarMetadata jar : jars)
                range = restrictRanges(range, jar.version().range());
            for (ContainedJarMetadata jar : extraRestrictions.getOrDefault(identifier, Collections.emptyList()))
                range = restrictRanges(range, jar.version().range());

            // No candidates, this is possible if a mod requests a dependency, but doesn't supply it
            if (jars.size() == 0) {
                options.add(new SelectionResult(identifier, Optional.empty(), false));
                failed = true;
                continue;
            }

            // No valid range found, we have to fail this
            if (range == null || !isValid(range)) {
                options.add(new SelectionResult(identifier, Optional.empty(), true));
                failed = true;
                continue;
            }

            // Only one choice, pick it
            if (jars.size() == 1) {
                ContainedJarMetadata jar = jars.iterator().next();
                if (range.containsVersion(jar.version().artifactVersion()))
                    options.add(new SelectionResult(identifier, Optional.of(jar), false));
                else {
                    options.add(new SelectionResult(identifier, Optional.empty(), false));
                    failed = true;
                }
                continue;
            }

            //If we have a recommended version, use that
            if (range.getRecommendedVersion() != null) {
                ContainedJarMetadata found = null;
                for (ContainedJarMetadata jar : jars) {
                    if (jar.version().artifactVersion().equals(range.getRecommendedVersion())) {
                        found = jar;
                        break;
                    }
                }

                if (found != null) {
                    options.add(new SelectionResult(identifier, Optional.of(found), false));
                    continue;
                }
            }

            // Find the highest available version in the range
            // Note the old version found the 'first' match. Which relied on implementation details of MultiHashMap, and can vary based on java versions.
            // If someone relied on that, screw them.
            ContainedJarMetadata found = null;
            for (ContainedJarMetadata jar : jars) {
                if (!range.containsVersion(jar.version().artifactVersion()))
                    continue;

                if (found == null || jar.version().artifactVersion().compareTo(found.version().artifactVersion()) > 0)
                    found = jar;
            }

            if (found == null)
                failed = true;

            options.add(new SelectionResult(identifier, Optional.ofNullable(found), false));
        }


        if (failed) {
            //We have entered into failure territory. Let's collect all of those that failed
            List<ResolutionFailureInformation<T>> failures = new ArrayList<>();
            for (SelectionResult result : options) {
                if (result.selected().isPresent())
                    continue;

                Set<SourceWithRequestedVersionRange<T>> sources = new HashSet<>();
                for (ContainedJarMetadata jar : metadataByIdentifier.get(result.identifier())) {
                    sources.add(new SourceWithRequestedVersionRange<T>(
                        detectedJarsBySource.get(jar).stream().map(DetectionResult::source).filter(s -> s != null).collect(Collectors.toSet()),
                        jar.version().range(),
                        jar.version().artifactVersion()
                    ));
                }

                failures.add(new ResolutionFailureInformation<>(getFailureReason(result), result.identifier(), sources));
            }

            LOGGER.error("Failed to select jars for {}", failures);
            return sneak(getFailureException(failures));
        }

        final List<T> selectedJars = new ArrayList<>(options.size());
        for (SelectionResult result : options) {
            ContainedJarMetadata meta = result.selected().orElse(null);

            if (meta == null)
                continue;

            Collection<DetectionResult<T>> sourceOfJar = detectedJarsBySource.get(meta);
            if (sourceOfJar == null || sourceOfJar.isEmpty())
                continue;

            // Lets pick the least nested source, should make extracting files faster
            DetectionResult<T> nearest = null;
            for (DetectionResult<T> info : sourceOfJar) {
                if (nearest == null || nearest.depth > info.depth)
                    nearest = info;
            }

            T winner = nearest == null ? null : nearest.source;
            if (winner != null)
                selectedJars.add(winner);
        }


        Map<String, T> seen = new HashMap<>();
        for (Iterator<T> itr = selectedJars.iterator(); itr.hasNext(); ) {
            T jar = itr.next();
            String id = this.getIdentifier(jar);

            T old = claimed.get(id);
            if (old != null) {
                LOGGER.warn("Attempted to select a dependency jar for JarJar which was passed in as source: {}. Using {}", id, old);
                itr.remove();
                continue;
            }

            old = seen.putIfAbsent(id, jar);
            if (old != null) {
                LOGGER.warn("Attempted to select two dependency jars from JarJar which have the same identification {}: {} and {}. Using {}", id, old, jar, old);
                itr.remove();
            }
        }

        return selectedJars;
    }

    @SuppressWarnings("unchecked")
    private static <R, E extends Throwable> R sneak(Throwable t) throws E {
        throw (E) t;
    }

    private static VersionRange restrictRanges(final VersionRange versionRange, final VersionRange versionRange2) {
        if (versionRange == null) {
            return versionRange2;
        }

        if (versionRange2 == null) {
            return versionRange;
        }

        return versionRange.restrict(versionRange2);
    }

    private static boolean isValid(final VersionRange range) {
        return range.getRecommendedVersion() == null && range.hasRestrictions();
    }

    private static FailureReason getFailureReason(SelectionResult selectionResult) {
        if (selectionResult.selected().isPresent())
            throw new IllegalArgumentException("Resolution succeeded, not failure possible");

        if (selectionResult.noValidRangeFound())
            return FailureReason.VERSION_RESOLUTION_FAILED;

        return FailureReason.NO_MATCHING_JAR;
    }

    private static final class DetectionResult<Z> {
        private final ContainedJarMetadata metadata;
        private final Z source;
        private final int depth;

        private DetectionResult(ContainedJarMetadata metadata, Z source, int depth) {
            this.metadata = metadata;
            this.source = source;
            this.depth = depth;
        }

        private Z source() {
            return source;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            final DetectionResult that = (DetectionResult) obj;
            return Objects.equals(this.metadata, that.metadata) &&
                    Objects.equals(this.source, that.source) &&
                    this.depth == that.depth;
        }

        @Override
        public int hashCode() {
            return Objects.hash(metadata, source, depth);
        }

        @Override
        public String toString() {
            return "DetectionResult[" +
                    "metadata=" + metadata + ", " +
                    "source=" + source + ", " +
                    "depth=" + depth + ']';
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final class SelectionResult {
        private final ContainedJarIdentifier identifier;
        private final Optional<ContainedJarMetadata> selected;
        private final boolean noValidRangeFound;

        private SelectionResult(ContainedJarIdentifier identifier, Optional<ContainedJarMetadata> selected, final boolean noValidRangeFound) {
            this.identifier = identifier;
            this.selected = selected;
            this.noValidRangeFound = noValidRangeFound;
        }

        public ContainedJarIdentifier identifier() {
            return identifier;
        }

        public Optional<ContainedJarMetadata> selected() {
            return selected;
        }

        public boolean noValidRangeFound() {
            return noValidRangeFound;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            final SelectionResult that = (SelectionResult) obj;
            return Objects.equals(this.identifier, that.identifier) &&
                    Objects.equals(this.selected, that.selected);
        }

        @Override
        public int hashCode() {
            return Objects.hash(identifier, selected);
        }

        @Override
        public String toString() {
            return "SelectionResult[" +
                    "identifier=" + identifier + ", " +
                    "selected=" + selected + ']';
        }
    }

    public enum FailureReason {
        VERSION_RESOLUTION_FAILED,
        NO_MATCHING_JAR,
    }

    public static final class SourceWithRequestedVersionRange<Z> {
        private final Collection<Z> sources;
        private final VersionRange requestedVersionRange;
        private final ArtifactVersion includedVersion;

        public SourceWithRequestedVersionRange(Collection<Z> sources, VersionRange requestedVersionRange, ArtifactVersion includedVersion) {
            this.sources = sources;
            this.requestedVersionRange = requestedVersionRange;
            this.includedVersion = includedVersion;
        }

        public Collection<Z> sources() {
            return sources;
        }

        public VersionRange requestedVersionRange() {
            return requestedVersionRange;
        }

        public ArtifactVersion includedVersion() {
            return includedVersion;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof SourceWithRequestedVersionRange)) return false;

            final SourceWithRequestedVersionRange<?> that = (SourceWithRequestedVersionRange<?>) o;

            if (!sources.equals(that.sources)) return false;
            if (!requestedVersionRange.equals(that.requestedVersionRange)) return false;
            return includedVersion.equals(that.includedVersion);
        }

        @Override
        public int hashCode() {
            int result = sources.hashCode();
            result = 31 * result + requestedVersionRange.hashCode();
            result = 31 * result + includedVersion.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "SourceWithRequestedVersionRange{" +
                    "source=" + sources +
                    ", requestedVersionRange=" + requestedVersionRange +
                    ", includedVersion=" + includedVersion +
                    '}';
        }
    }

    public static final class ResolutionFailureInformation<Z> {
        private final FailureReason failureReason;
        private final ContainedJarIdentifier identifier;
        private final Collection<SourceWithRequestedVersionRange<Z>> sources;

        public ResolutionFailureInformation(final FailureReason failureReason, final ContainedJarIdentifier identifier, final Collection<SourceWithRequestedVersionRange<Z>> sources) {
            this.failureReason = failureReason;
            this.identifier = identifier;
            this.sources = sources;
        }

        public FailureReason failureReason() {
            return failureReason;
        }

        public ContainedJarIdentifier identifier() {
            return identifier;
        }

        public Collection<SourceWithRequestedVersionRange<Z>> sources() {
            return sources;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof ResolutionFailureInformation)) return false;

            final ResolutionFailureInformation<?> that = (ResolutionFailureInformation<?>) o;

            if (failureReason != that.failureReason) return false;
            if (!identifier.equals(that.identifier)) return false;
            return sources.equals(that.sources);
        }

        @Override
        public int hashCode() {
            int result = failureReason.hashCode();
            result = 31 * result + identifier.hashCode();
            result = 31 * result + sources.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "ResolutionFailureInformation{" +
                    "failureReason=" + failureReason +
                    ", identifier=" + identifier +
                    ", sources=" + sources +
                    '}';
        }
    }
}
