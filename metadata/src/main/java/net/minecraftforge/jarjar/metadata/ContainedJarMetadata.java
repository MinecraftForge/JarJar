/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.metadata;

import org.apache.maven.artifact.versioning.VersionRange;

import java.util.Objects;

public final class ContainedJarMetadata
{
    private final ContainedJarIdentifier identifier;
    private final ContainedVersion       version;
    private final String                 path;
    private final boolean                isObfuscated;

    public ContainedJarMetadata(ContainedJarIdentifier identifier, ContainedVersion version, String path, boolean isObfuscated)
    {
        this.identifier = identifier;
        this.version = version;
        this.path = path;
        this.isObfuscated = isObfuscated;
    }

    public ContainedJarIdentifier identifier() {return identifier;}

    public ContainedVersion version() {return version;}

    public String path() {return path;}

    public boolean isObfuscated() {return isObfuscated;}

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final ContainedJarMetadata that = (ContainedJarMetadata) obj;
        return Objects.equals(this.identifier, that.identifier) &&
                 Objects.equals(this.version, that.version) &&
                 Objects.equals(this.path, that.path) &&
                 this.isObfuscated == that.isObfuscated;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(identifier, version, path, isObfuscated);
    }

    @Override
    public String toString()
    {
        return "ContainedJarMetadata[" +
                 "identifier=" + identifier + ", " +
                 "version=" + version + ", " +
                 "path=" + path + ", " +
                 "isObfuscated=" + isObfuscated + ']';
    }
}
