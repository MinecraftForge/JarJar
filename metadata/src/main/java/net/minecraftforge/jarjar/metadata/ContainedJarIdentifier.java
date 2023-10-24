/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.metadata;

import java.util.Objects;

public final class ContainedJarIdentifier
{
    private final String group;
    private final String artifact;

    public ContainedJarIdentifier(String group, String artifact)
    {
        this.group = group;
        this.artifact = artifact;
    }

    public String group() {return group;}

    public String artifact() {return artifact;}

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final ContainedJarIdentifier that = (ContainedJarIdentifier) obj;
        return Objects.equals(this.group, that.group) &&
                 Objects.equals(this.artifact, that.artifact);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(group, artifact);
    }

    @Override
    public String toString()
    {
        return "ContainedJarIdentifier[" +
                 "group=" + group + ", " +
                 "artifact=" + artifact + ']';
    }
}
