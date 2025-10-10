/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.selector;

import net.minecraftforge.jarjar.metadata.*;
import net.minecraftforge.jarjar.selection.JarSelector;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;


public class JarSelectorTest {

    @Test
    public void doesSelectRestrictedRange() throws InvalidVersionSpecificationException {
        final List<SelectionSource> sources = new ArrayList<>();
        sources.add(createSource("outer_lower_version", createArtifact("[1.0.0,)", "1.0.0"), "test.one"));
        sources.add(createSource("outer_higher_version", createArtifact("[1.0.1,)", "1.0.1"), "test.one"));

        final List<SelectionSource> selectedSources = process(sources);

        Assertions.assertEquals(1, selectedSources.size());
    }

    @Test
    public void twoExactSameInnersResolvesToOne() throws InvalidVersionSpecificationException {
        final List<SelectionSource> sources = new ArrayList<>();
        sources.add(createSource("outer_one", createArtifact("[1.0.0,)", "1.0.0"), "test.one"));
        sources.add(createSource("outer_two", createArtifact("[1.0.0,)", "1.0.0"), "test.one"));

        final List<SelectionSource> selectedSources = process(sources);

        Assertions.assertEquals(1, selectedSources.size());
    }

    @Test
    public void doesSelectThrowFailureIfNotUniteable() throws InvalidVersionSpecificationException {
        final List<SelectionSource> sources = new ArrayList<>();
        sources.add(createSource("outer_lower_version", createArtifact("[1.0.0,1.5.0)", "1.0.0"), "test.one"));
        sources.add(createSource("outer_higher_version", createArtifact("[2.0.1,)", "1.0.1"), "test.one"));

        Assertions.assertThrows(IllegalStateException.class, () -> {
            process(sources);
        });
    }

    @Test
    public void doesSelectThrowFailureIfNoArtifactFound() throws InvalidVersionSpecificationException {
        final List<SelectionSource> sources = new ArrayList<>();
        sources.add(createSource("outer_lower_version", createArtifact("[1.0.0,1.5.0)", "1.0.0"), "test.one"));
        sources.add(createSource("outer_higher_version", createArtifact("[1.4.0,1.6.0)", "1.5.5"), "test.one"));

        Assertions.assertThrows(IllegalStateException.class, () -> {
            process(sources);
        });
    }

    @Test
    public void selectSelectsNothingWhenNoInput() {
        final List<SelectionSource> sources = new ArrayList<>();

        final List<SelectionSource> selectedSources = process(sources);

        Assertions.assertEquals(0, selectedSources.size());
    }

    @Test
    public void doesSelectSelectAllDifferentArtifacts() throws InvalidVersionSpecificationException {
        final List<SelectionSource> sources = new ArrayList<>();
        sources.add(createSource("outer_lower_version", createArtifact("[1.0.0,)", "1.0.0"), "test.one"));
        sources.add(createSource("outer_higher_version", createArtifact("test.two", "[1.0.1,)", "1.0.1"), "test.two"));

        final List<SelectionSource> selectedSources = process(sources);

        Assertions.assertEquals(2, selectedSources.size());
    }


    @Test
    public void allowsUpgrade() throws InvalidVersionSpecificationException {
        SelectionSource outer = new SelectionSource("test.one");
        SelectionSource inner = new SelectionSource("test.one");

        JarSelector<SelectionSource> selector = new Selector();
        selector.add(createSource("wrapper", createArtifact("[1.0,)", "1.1"), inner));
        selector.option(outer, createArtifact("[0,)", "1.0"));

        List<SelectionSource> selectedSources = selector.select();

        Assertions.assertEquals(1, selectedSources.size());
        Assertions.assertEquals(inner, selectedSources.get(0), "Expected inner to be selected, but outer was");
    }

    @Test
    public void forcesDowngrade() throws InvalidVersionSpecificationException {
        JarSelector<SelectionSource> selector = new Selector();
        selector.add(createSource("wrapper", createArtifact("[1.0,)", "1.1"), "test.one"));
        selector.force(new SelectionSource("test.one"));

        List<SelectionSource> selectedSources = selector.select();

        Assertions.assertEquals(0, selectedSources.size(), "Expected no result, as the outer was forced.");
    }


    private SelectionSource createSource(final String outer_name, final ContainedJarMetadata artifact, final String inner_name) throws InvalidVersionSpecificationException {
        return createSource(outer_name, artifact, new SelectionSource(inner_name));
    }

    private SelectionSource createSource(final String outer_name, final ContainedJarMetadata artifact, final SelectionSource inner) throws InvalidVersionSpecificationException {
        return new SelectionSource(outer_name, new Metadata(Arrays.asList(artifact)), inner);
    }

    private ContainedJarMetadata createArtifact(final String spec, final String version) throws InvalidVersionSpecificationException {
        return createArtifact("test.one", spec, version);
    }
    private ContainedJarMetadata createArtifact(final String name, final String spec, final String version) throws InvalidVersionSpecificationException {
        return new ContainedJarMetadata(
                new ContainedJarIdentifier(name, "artifact"),
                new ContainedVersion(VersionRange.createFromVersionSpec(spec), new DefaultArtifactVersion(version)),
                name,
                false
        );
    }

    private static List<SelectionSource> process(List<SelectionSource> sources) {
        JarSelector<SelectionSource> selector = new Selector();
        selector.add(sources);
        return selector.select();
    }

    private static class Selector extends JarSelector<SelectionSource> {
        @Override
        @Nullable
        protected InputStream getResource(SelectionSource source, String path) {
            return source.getResource(path).orElse(null);
        }

        @Override
        @Nullable
        protected SelectionSource getNested(SelectionSource source, String path) {
            return source.getInternal(path).orElse(null);
        }

        @Override
        protected String getIdentifier(SelectionSource source) {
            return source.getName();
        }

        @Override
        protected Throwable getFailureException(Collection<ResolutionFailureInformation<SelectionSource>> failures) {
            return new IllegalStateException("Failed");
        }
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

        public Optional<InputStream> getResource(String path) {
            if (metadata != null && path.replace('\\', '/').endsWith(JarSelector.CONTAINED_JARS_METADATA_PATH))
                return Optional.of(MetadataIOHandler.toInputStream(metadata));

            return Optional.empty();
        }

        public Optional<SelectionSource> getInternal(String path) {
            if (internalSource != null && path.contains(internalSource.getName()))
                return Optional.of(internalSource);

            return Optional.empty();
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return getName() + "@" + Integer.toHexString(hashCode());
        }
    }
}
