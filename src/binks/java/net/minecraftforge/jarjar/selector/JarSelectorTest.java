package net.minecraftforge.jarjar.selector;

import com.google.common.collect.ImmutableList;
import net.minecraftforge.jarjar.metadata.*;
import net.minecraftforge.jarjar.selection.JarSelector;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class JarSelectorTest {

    @Test
    public void doesSelectRestrictedRange() throws InvalidVersionSpecificationException {
        final List<SelectionSource> sources = new ArrayList<>();
        sources.add(new SelectionSource(
                "outer_lower_version",
                new Metadata(
                ImmutableList.of(new ContainedJarMetadata(
                        new ContainedJarIdentifier("test.one", "artifact"),
                        new ContainedVersion(VersionRange.createFromVersionSpec("[1.0.0,)"), new DefaultArtifactVersion("1.0.0")),
                        "test.one",
                        false
                ))),
                new SelectionSource("inner_lower_version")));
        sources.add(new SelectionSource(
                "outer_higher_version",
                new Metadata(
                        ImmutableList.of(new ContainedJarMetadata(
                                new ContainedJarIdentifier("test.one", "artifact"),
                                new ContainedVersion(VersionRange.createFromVersionSpec("[1.0.1,)"), new DefaultArtifactVersion("1.0.1")),
                                "test.one",
                                false
                        ))),
                new SelectionSource("inner_higher_version")));

        final List<SelectionSource> selectedSources = JarSelector.detectAndSelect(
                sources,
                SelectionSource::getResource,
                SelectionSource::getInternal,
                SelectionSource::getName,
                (Function<Collection<JarSelector.ResolutionFailureInformation<SelectionSource>>, IllegalStateException>) resolutionFailureInformations -> {
                    throw new IllegalStateException("Failed");
                }
        );

        Assertions.assertEquals(1, selectedSources.size());
    }

    private final class SelectionSource {

        private final String name;
        private Metadata metadata = null;
        private SelectionSource internalSource = null;

        public SelectionSource(final String name, final Metadata metadata, final SelectionSource source) {
            this.metadata = metadata;
            this.internalSource = source;
            this.name = name;
        }

        public SelectionSource(final String name) {
            this.name = name;
        }

        public Optional<InputStream> getResource(final Path path) {
            if (path.toString().contains("metadata") && metadata != null) {
                return Optional.of(MetadataIOHandler.toInputStream(metadata));
            }

            return Optional.empty();
        }

        public Optional<SelectionSource> getInternal(final Path path) {
            if (path.toString().contains("test.one")) {
                return Optional.ofNullable(internalSource);
            }

            return Optional.empty();
        }

        public String getName() {
            return name;
        }
    }
}
