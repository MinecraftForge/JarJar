package net.minecraftforge.spi.jij;

import org.apache.maven.artifact.versioning.VersionRange;

public record ContainedJar(ContainedJarIdentifier identifier, VersionRange version, String sha256Hash, String path)
{
}
