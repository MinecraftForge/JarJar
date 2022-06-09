package net.minecraftforge.jarjar.metadata;

import java.util.List;
import java.util.Objects;

public final class Metadata
{
    private final List<ContainedJarMetadata> jars;

    public Metadata(List<ContainedJarMetadata> jars) {this.jars = jars;}

    public List<ContainedJarMetadata> jars() {return jars;}

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final Metadata that = (Metadata) obj;
        return Objects.equals(this.jars, that.jars);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(jars);
    }

    @Override
    public String toString()
    {
        return "Metadata[" +
                 "jars=" + jars + ']';
    }
}
