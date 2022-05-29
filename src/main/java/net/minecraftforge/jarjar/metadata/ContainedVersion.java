package net.minecraftforge.jarjar.metadata;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;

public record ContainedVersion(VersionRange range, ArtifactVersion artifactVersion)
{
}
