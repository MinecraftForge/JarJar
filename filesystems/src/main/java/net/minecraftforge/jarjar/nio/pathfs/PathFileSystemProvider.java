/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.nio.pathfs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PathFileSystemProvider extends FileSystemProvider {
    protected static final String COMPONENT_SEPERATOR = "~";
    public static final String PATH_SEPERATOR = PathFileSystemProvider.COMPONENT_SEPERATOR + "/";
    private final Map<String, PathFileSystem> fileSystems = new HashMap<>();

    @Override
    public String getScheme() {
        return "path";
    }

    /**
     * Invoked by FileSystems.newFileSystem, Only returns a value if env contains an entry with the name of
     *          "packagePath" and Path targeting the file in question.
     * If not specified, throws IllegalArgumentException
     * If uri.getScheme() is not "path" throws IllegalArgumentException
     * If you wish to create a PathFileSystem explicitly, invoke newFileSystem(Path)
     */
    @Override
    public FileSystem newFileSystem(final URI uri, final Map<String, ?> env) throws IOException {
        @SuppressWarnings("unchecked")
        final Path packagePath = ((Map<String, Path>)env).getOrDefault("packagePath", null);

        if (packagePath == null)
            throw new UnsupportedOperationException("Missing packagePath");

        final String key = makeKey(uri);

        if (this.fileSystems.containsKey(key))
            return this.fileSystems.get(key);

        try {
            return newFileSystemInternal(key, packagePath);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Invoked by FileSystems.newFileSystem, Only returns a value if env contains an entry with the name of
     *          "packagePath" and Path targeting the file in question.
     * If none specified, throws UnsupportedOperationException instead of IllegalArgumentException
     *   so that FileSystems.newFileSystem will search for the next provider.
     */
    @Override
    public FileSystem newFileSystem(final Path path, final Map<String, ?> env) throws IOException {
        @SuppressWarnings("unchecked")
        final Path packagePath = ((Map<String, Path>)env).getOrDefault("packagePath", null);

        if (packagePath == null)
            throw new UnsupportedOperationException("Missing packagePath");

        final String key = makeKey(path);
        try {
            return newFileSystemInternal(key, packagePath);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public PathFileSystem newFileSystem(final Path path) {
        if (path == null) throw new IllegalArgumentException("Path is null");
        final String key = makeKey(path);
        return newFileSystemInternal(key, path);
    }

    private PathFileSystem newFileSystemInternal(final String key, final Path path) {
        final Path normalizedPath = path.toAbsolutePath().normalize();

        synchronized (fileSystems) {
            final PathFileSystem ufs = new PathFileSystem(this, key, normalizedPath);
            fileSystems.put(key, ufs);
            return ufs;
        }
    }

    private String makeKey(URI uri) {
        final String keyValue = uri.normalize().getRawSchemeSpecificPart();

        // TODO: [JJ] Remove this crap
        if (keyValue.startsWith("//"))
            return keyValue.substring(2);

        return keyValue;
    }

    private String makeKey(Path path) {
        return path.getFileName().toString();
    }

    private static class URIComponents {
        String owner;
        String path;

        URIComponents(String owner, String path) {
            this.owner = owner;
            this.path = path;
        }
    }

    private URIComponents parse(URI uri) {
        String key = makeKey(uri);
        int idx = key.lastIndexOf(COMPONENT_SEPERATOR);
        if (idx == -1)
            return new URIComponents(key, null);
        String owner = key.substring(0, idx);
        String path = key.substring(idx + 1);
        return new URIComponents(owner, path);
    }


    @Override
    public Path getPath(final URI uri) {
        URIComponents parts = parse(uri);
        if (parts.path != null)
            return getFileSystem(uri).getPath(parts.path);
        else
            return ((PathFileSystem)getFileSystem(uri)).getRoot();
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        URIComponents parts = parse(uri);
        FileSystem fs = fileSystems.get(parts.owner);

        if (fs == null) {
            StringBuilder buf = new StringBuilder();
            buf.append("Unknown FileSystem: ").append(uri);
            buf.append('\n').append("\tOwner: ").append(parts.owner);
            List<String> sorted = new ArrayList<>(fileSystems.keySet());
            Collections.sort(sorted);
            for (String known : sorted)
                buf.append('\n').append("\tKnown: ").append(known);
            throw new FileSystemNotFoundException(buf.toString());
        }

        return fs;
    }

    @Override
    public SeekableByteChannel newByteChannel(final Path path, final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {
        if (path instanceof PathPath) {
            final PathPath up = (PathPath) path;
            return up.getFileSystem().newByteChannel(path, options, attrs);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir, final DirectoryStream.Filter<? super Path> filter) throws IOException {
        if (dir instanceof PathPath) {
            final PathPath up = (PathPath) dir;
            return up.getFileSystem().newDirectoryStream(dir, filter);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(final Path path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copy(final Path source, final Path target, final CopyOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(final Path source, final Path target, final CopyOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSameFile(final Path path, final Path path2) throws IOException {
        return false;
    }

    @Override
    public boolean isHidden(final Path path) throws IOException {
        return false;
    }

    @Override
    public FileStore getFileStore(final Path path) throws IOException {
        return null;
    }

    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        if (path instanceof PathPath) {
            final PathPath up = (PathPath) path;
            up.getFileSystem().checkAccess(path, modes);
            return;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(final Path path, final Class<V> type, final LinkOption... options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(final Path path, final Class<A> type, final LinkOption... options) throws IOException {
        if (path instanceof PathPath) {
            final PathPath p = (PathPath) path;
            return p.getFileSystem().readAttributes(path, type, options);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> readAttributes(final Path path, final String attributes, final LinkOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAttribute(final Path path, final String attribute, final Object value, final LinkOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    void removeFileSystem(PathFileSystem fs) {
        synchronized (fileSystems) {
            fileSystems.remove(fs.getKey());
        }
    }

    protected URI buildUriFor(final PathPath path) throws URISyntaxException, IllegalArgumentException {
        return new URI(
            path.getFileSystem().provider().getScheme(),
            path.getFileSystem().getKey() + PATH_SEPERATOR + path,
            null
        );
    }

    protected Path createSubPath(final PathFileSystem pathFileSystem, final String... args) {
        return new PathPath(pathFileSystem, false, args);
    }

    public Path adaptResolvedPath(final PathPath path) {
        return path;
    }

    public String[] adaptPathParts(final String longstring, final String[] pathParts) {
        return pathParts;
    }

    protected Optional<FileSystem> getFileSystemFromKey(final String section) {
        return Optional.ofNullable(this.fileSystems.get(section));
    }
}
