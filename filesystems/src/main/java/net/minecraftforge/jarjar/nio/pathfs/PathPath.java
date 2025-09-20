/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.nio.pathfs;

import net.minecraftforge.jarjar.nio.AbstractPath;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Objects;
import java.util.function.IntBinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PathPath extends AbstractPath implements Path {
    private final PathFileSystem fileSystem;
    private final String[] pathParts;
    public static final String ROOT = "/";

    PathPath(final PathFileSystem fileSystem, boolean knownCorrectSplit, final String... pathParts) {
        this.fileSystem = fileSystem;
        if (pathParts.length == 0)
            this.pathParts = new String[0];
        else if (knownCorrectSplit)
            this.pathParts = pathParts;
        else {
            final String longstring = String.join(fileSystem.getSeparator(), pathParts);
            this.pathParts = getPathParts(longstring);
        }
    }

    protected PathPath(final PathFileSystem fileSystem, final Path innerPath) {
        this.fileSystem = fileSystem;
        this.pathParts = innerPath.toString().replace("\\", "/").split("/");
    }

    private String[] getPathParts(String longstring) {
        String sep = this.getFileSystem().getSeparator();
        String[] localParts = longstring.equals(sep) ? new String[] {""} : longstring.replace("\\", sep).split(sep);

        if (localParts.length > 1 && localParts[0].isEmpty())
            localParts = Arrays.copyOfRange(localParts, 1, localParts.length);

        if (this.getFileSystem().provider() != null)
            return this.getFileSystem().provider().adaptPathParts(longstring, localParts);

        return localParts;
    }

    @Override
    public PathFileSystem getFileSystem() {
        return this.fileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return this.pathParts.length == 0 || this.pathParts[0].isEmpty();
    }

    @Override
    public Path getRoot() {
        return this.fileSystem.getRoot();
    }

    @Override
    public Path getFileName() {
        if (this.getRoot().equals(this.toAbsolutePath())) {
            //We are root. To allow FML to load generic libraries into ML we need a proper name here.
            return this.fileSystem.getTarget().getFileName();
        }

        return this.pathParts.length > 0 ? new PathPath(this.getFileSystem(), true, this.pathParts[this.pathParts.length - 1]) : new PathPath(this.fileSystem, true, "");
    }

    @Override
    public Path getParent() {
        if (this.pathParts.length > 0 && !(pathParts.length == 1 && pathParts[0].isEmpty()))
            return new PathPath(this.fileSystem, true, Arrays.copyOf(this.pathParts, this.pathParts.length - 1));
        return null;
    }

    @Override
    public int getNameCount() {
        return this.pathParts.length;
    }

    @Override
    public Path getName(final int index) {
        if (index < 0 || index > this.pathParts.length - 1)
            throw new IllegalArgumentException();
        return new PathPath(this.fileSystem, true, this.pathParts[index]);
    }

    @Override
    public Path subpath(final int beginIndex, final int endIndex) {
        if (beginIndex < 0 || beginIndex > this.pathParts.length - 1 || endIndex < 0 || endIndex > this.pathParts.length || beginIndex > endIndex)
            throw new IllegalArgumentException("Out of range " + beginIndex + " to " + endIndex + " for length " + this.pathParts.length);
        return new PathPath(this.fileSystem, true, Arrays.copyOfRange(this.pathParts, beginIndex, endIndex));
    }

    @Override
    public boolean startsWith(final Path other) {
        if (other.getFileSystem() != this.getFileSystem())
            return false;

        if (other instanceof PathPath) {
            final PathPath bp = (PathPath) other;
            return checkArraysMatch(this.pathParts, bp.pathParts, false);
        }

        return false;
    }


    @Override
    public boolean endsWith(final Path other) {
        if (other.getFileSystem() != this.getFileSystem())
            return false;

        if (other instanceof PathPath) {
            final PathPath bp = (PathPath) other;
            return checkArraysMatch(this.pathParts, bp.pathParts, true);
        }

        return false;
    }

    private static boolean checkArraysMatch(String[] array1, String[] array2, boolean reverse) {
        final int length = Math.min(array1.length, array2.length);
        IntBinaryOperator offset = reverse ? (l, i) -> l - i - 1 : (l, i) -> i;
        for (int i = 0; i < length; i++) {
            if (!Objects.equals(array1[offset.applyAsInt(array1.length, i)], array2[offset.applyAsInt(array2.length, i)]))
                return false;
        }
        return true;
    }

    @Override
    public Path normalize() {
        Deque<String> normpath = new ArrayDeque<>();
        for (String pathPart : this.pathParts) {
            switch (pathPart) {
                case ".":
                    break;
                case "..":
                    normpath.removeLast();
                    break;
                default:
                    normpath.addLast(pathPart);
                    break;
            }
        }
        return new PathPath(this.fileSystem, true, normpath.toArray(new String[0]));
    }

    @Override
    public Path resolve(final Path other) {
        if (other instanceof PathPath) {
            final PathPath path = (PathPath) other;
            PathFileSystemProvider provider = this.getFileSystem().provider();

            if (path.isAbsolute())
                return provider.adaptResolvedPath(path);

            return provider.adaptResolvedPath(new PathPath(this.fileSystem, false, this + fileSystem.getSeparator() + other));
        }
        return other;
    }

    @Override
    public Path relativize(final Path other) {
        if (other.getFileSystem() != this.getFileSystem()) throw new IllegalArgumentException("Wrong filesystem");
        if (this.equals(getRoot()) && other.equals(other.getRoot())) {
            return this;
        }
        if (other instanceof PathPath) {
            final PathPath p = (PathPath) other;
            final int poff = p.isAbsolute() ? 1 : 0;
            final int meoff = this.isAbsolute() ? 1 : 0;
            final int length = Math.min(this.pathParts.length - meoff, p.pathParts.length - poff);
            int i = 0;
            while (i < length) {
                if (!Objects.equals(this.pathParts[i + meoff], p.pathParts[i + poff]))
                    break;
                i++;
            }

            final int remaining = this.pathParts.length - i - meoff;
            if (remaining == 0 && i == p.pathParts.length) {
                return new PathPath(this.getFileSystem(), false);
            } else if (remaining == 0) {
                return p.subpath(i + 1, p.getNameCount());
            } else {
                final String updots = IntStream.range(0, remaining).mapToObj(idx -> "..").collect(Collectors.joining(getFileSystem().getSeparator()));
                if (i == p.pathParts.length) {
                    return new PathPath(this.getFileSystem(), false, updots);
                } else {
                    return new PathPath(this.getFileSystem(), false, updots + getFileSystem().getSeparator() + p.subpath(i, p.getNameCount()));
                }
            }
        }
        throw new IllegalArgumentException("Wrong filesystem");
    }

    @Override
    public URI toUri() {
        try {
            return fileSystem.provider().buildUriFor(this);
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Path toAbsolutePath() {
        if (isAbsolute())
            return this;
        else
            return fileSystem.getRoot().resolve(this);
    }

    @Override
    public Path toRealPath(final LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public WatchKey register(final WatchService watcher, final WatchEvent.Kind<?>[] events, final WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(final Path other) {
        return 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof PathPath) {
            final PathPath p = (PathPath) o;
            return p.getFileSystem() == this.getFileSystem() && Arrays.equals(this.pathParts, p.pathParts);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.fileSystem) + 31 * Arrays.hashCode(this.pathParts);
    }

    @Override
    public String toString() {
        return String.join(fileSystem.getSeparator(), this.pathParts).replace("//", "/");
    }
}
