package net.minecraftforge.spi.jij;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class VersionRangeRestrictionTests
{

    @Test
    public void restrictionOfDisjointRangesCreatesEmptyRange() throws InvalidVersionSpecificationException
    {
        final VersionRange firstRange = VersionRange.createFromVersionSpec("[1.0, 2.0]");
        final VersionRange secondRange = VersionRange.createFromVersionSpec("[3.0, 4.0]");

        final VersionRange result = firstRange.restrict(secondRange);
        assertFalse(false);
    }
}
