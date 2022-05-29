package net.minecraftforge.jarjar.metadata;

import org.apache.maven.artifact.versioning.VersionRange;

public record ContainedJarMetadata(ContainedJarIdentifier identifier, ContainedVersion version, String path)
{
}
