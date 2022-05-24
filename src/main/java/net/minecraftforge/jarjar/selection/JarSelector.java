package net.minecraftforge.jarjar.selection;

import net.minecraftforge.jarjar.metadata.ContainedJar;
import net.minecraftforge.jarjar.metadata.ContainedJarIdentifier;
import org.apache.maven.artifact.versioning.VersionRange;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class JarSelector
{
    private JarSelector()
    {
        throw new IllegalStateException("Can not instantiate an instance of: JarSelector. This is a utility class");
    }

    public static List<ContainedJar> select(final List<ContainedJar> inputs) {
        final Map<ContainedJarIdentifier, List<ContainedJar>> jarsByIdentifier = inputs.stream()
                                                                                   .collect(Collectors.groupingBy(ContainedJar::identifier));

        return jarsByIdentifier.values().stream()
                 .filter(jars -> !jars.isEmpty())
                 .flatMap(jars -> {
                     if (jars.size() == 1) {
                         //Quick return:
                         return jars.stream();
                     }

                     //Find the most agreeable version:
                     final VersionRange range = jars.stream()
                                                  .map(ContainedJar::version)
                                                  .reduce(null, JarSelector::restrictRanges);

                     if (!isValid(range)) {
                         return Stream.empty();
                     }

                     if (range.getRecommendedVersion() != null) {
                         return jars.stream().filter(jar -> jar.version().getRecommendedVersion().equals(range.getRecommendedVersion())).findFirst().stream();
                     }

                     return jars.stream().filter(jar -> range.containsVersion(jar.version().getRecommendedVersion())).findFirst().stream();
                 })
                 .collect(Collectors.toList());
    }

    private static VersionRange restrictRanges(final VersionRange versionRange, final VersionRange versionRange2)
    {
        if (versionRange == null) {
            return versionRange2;
        }

        if (versionRange2 == null) {
            return versionRange;
        }

        return versionRange.restrict(versionRange);
    }

    private static boolean isValid(final VersionRange range) {
        return range.getRecommendedVersion() == null && !range.hasRestrictions();
    }
}
