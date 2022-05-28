package net.minecraftforge.jarjar.metadata;

import org.apache.maven.artifact.versioning.VersionRange;

public record ContainedJarMetadata(ContainedJarIdentifier identifier, VersionRange version, String path)
{
}
