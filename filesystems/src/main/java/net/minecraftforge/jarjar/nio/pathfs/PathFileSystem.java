/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.nio.pathfs;

import net.minecraftforge.jarjar.nio.util.LambdaExceptionUtils;
import net.minecraftforge.jarjar.nio.util.Lazy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class PathFileSystem extends FileSystem {
    private final Path root = new PathPath(this, false, PathPath.ROOT).toAbsolutePath();
    private final PathFileSystemProvider provider;
    private final String key;
    private final Path target;
    private final Lazy<FileSystem> innerSystem;
    private final Lazy<Path> innerFSTarget;

    PathFileSystem(PathFileSystemProvider provider, String key, Path target) {
        this.provider = provider;
        this.key = key;
        this.target = target;

        this.innerSystem = Lazy.of(() -> {
            try {
                return FileSystems.newFileSystem(target, this.getClass().getClassLoader());
            } catch (IOException e) {
                return sneak(e);
                //return target.getFileSystem();
            }
        });

        this.innerFSTarget = this.innerSystem.map(fileSystem -> {
            if (fileSystem == target.getFileSystem())
                return target;

            //We need to process the new FS root directories.
            //We do this since creating an FS from a zip file changes the root to which we need to make our inner paths relative.
            final List<Path> roots = new ArrayList<>();
            fileSystem.getRootDirectories().forEach(roots::add);

            if (roots.size() != 1)
                throw new UnsupportedOperationException("Invalid PathFileSystem, Multiple roots: " + target.toUri());

            return roots.get(0);
        });
    }

    public String getKey() {
        return this.key;
    }

    public Path getRoot() {
        return root;
    }

    @Override
    public PathFileSystemProvider provider() {
        return provider;
    }

    @Override
    public void close() {
        innerSystem.ifPresent(LambdaExceptionUtils.uncheckConsume(FileSystem::close));
        provider().removeFileSystem(this);
    }

    @Override
    public boolean isOpen() {
        return innerSystem.map(FileSystem::isOpen).orElse(true);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        if (path.toAbsolutePath().equals(root))
            return Files.readAttributes(this.target, type, options);

        return innerSystem.get().provider().readAttributes(getOuterTarget(path), type, options);
    }

    @Override
    public String getSeparator() {
        return "/";
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singletonList(root);
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return Collections.emptyList();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Collections.singleton("basic");
    }

    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        if (path.toAbsolutePath().equals(root)) {
            try {
                return Files.newByteChannel(this.target, options, attrs);
            } catch (UncheckedIOException ioe) {
                throw ioe.getCause();
            }
        }

        return this.innerSystem.get().provider().newByteChannel(getOuterTarget(path), options, attrs);
    }

    private Path getOuterTarget(Path path) {
        if (path.isAbsolute())
            path = root.relativize(path);

        Path innerRoot = this.innerFSTarget.get();
        return innerRoot.resolve(path.toString());
    }

    @Override
    public Path getPath(String first, String... more) {
        if (more.length > 0) {
            final String[] args = new String[more.length + 1];
            args[0] = first;
            System.arraycopy(more, 0, args, 1, more.length);

            return provider().createSubPath(this, args);
        }
        return provider().createSubPath(this, first);
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchService newWatchService() {
        throw new UnsupportedOperationException();
    }

    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) {
        if (dir.toAbsolutePath().equals(root)) {
            try {
                return PathFSUtils.adapt(
                    Files.newDirectoryStream(this.innerFSTarget.get(), filter),
                    path -> new PathPath(this, this.innerFSTarget.get().relativize(path))
                );
            } catch (IOException e) {
                return PathFSUtils.NULL_STREAM;
            }
        }

        try {
            return PathFSUtils.adapt(
                this.innerSystem.get().provider().newDirectoryStream(getOuterTarget(dir), filter),
                path -> new PathPath(this, target.relativize(path))
            );
        } catch (IOException e) {
            return PathFSUtils.NULL_STREAM;
        }
    }

    public Path getTarget() {
        return target;
    }

    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        innerSystem.get().provider().checkAccess(getOuterTarget(path), modes);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R sneak(Exception exception) throws E {
        throw (E) exception;
    }
}
