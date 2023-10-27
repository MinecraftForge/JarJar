/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.metadata;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;

import java.util.Objects;

public final class ContainedVersion
{
    private final VersionRange    range;
    private final ArtifactVersion artifactVersion;

    public ContainedVersion(VersionRange range, ArtifactVersion artifactVersion)
    {
        this.range = range;
        this.artifactVersion = artifactVersion;
    }

    public VersionRange range() {return range;}

    public ArtifactVersion artifactVersion() {return artifactVersion;}

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final ContainedVersion that = (ContainedVersion) obj;
        return Objects.equals(this.range, that.range) &&
                 Objects.equals(this.artifactVersion, that.artifactVersion);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(range, artifactVersion);
    }

    @Override
    public String toString()
    {
        return "ContainedVersion[" +
                 "range=" + range + ", " +
                 "artifactVersion=" + artifactVersion + ']';
    }
}
