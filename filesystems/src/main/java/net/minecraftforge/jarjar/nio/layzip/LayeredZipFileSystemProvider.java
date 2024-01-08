/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.nio.layzip;

import net.minecraftforge.jarjar.nio.pathfs.PathFileSystemProvider;
import net.minecraftforge.jarjar.nio.pathfs.PathPath;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class LayeredZipFileSystemProvider extends PathFileSystemProvider {
    public static final String SCHEME = "jij";
    public static final String URI_SPLIT_REGEX = COMPONENT_SEPERATOR;

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public FileSystem newFileSystem(final URI uri, final Map<String, ?> env) throws IOException {
        final String[] sections = uri.getRawSchemeSpecificPart().split(URI_SPLIT_REGEX);

        FileSystem workingSystem = FileSystems.getDefault(); //Grab the normal disk FS.
        String keyPrefix = "";

        if (sections.length > 1) {
            final AdaptedURIWithPrefixSelection adaptedURI = adaptUriSections(sections);
            keyPrefix = adaptedURI.getPrefix();
            workingSystem = adaptedURI.getFileSystem();
        }

        String lastSection = sections[sections.length - 1];
        if (lastSection.startsWith("//"))
            lastSection = lastSection.substring(2);

        if (env.containsKey("packagePath")) { //User requests specific package as a target;
            try {
                return super.newFileSystem(new URI(super.getScheme() + ":" + uri.getRawSchemeSpecificPart()), env);
            } catch (Exception e) {
                throw new UncheckedIOException("Failed to create intermediary FS.", new IOException("Failed to process data.", e));
            }
        }

        final Path lastPath = workingSystem.getPath(lastSection).toAbsolutePath();
        return getOrCreateNewSystem(keyPrefix, lastPath);
    }

    private String handleAbsolutePrefixOnWindows(final FileSystem workingSystem, String section) {
        if (workingSystem.getClass().getName().toLowerCase(Locale.ROOT).contains("windows"))
        {
            //This special casing is needed, since else the rooted paths crash on Windows system because:
            // /D:/something is not a valid path on Windows.
            //However, the JDK does not expose the Windows FS types and there are no marker classes, so we use the
            // classname.
            //Because we are fancy like that.
            if (section.startsWith("/"))
                section = section.substring(1); //Turns /D:/something into D:/Something which is a valid windows path.
        }
        return section;
    }

    private FileSystem getOrCreateNewSystem(Path path) {
        return getOrCreateNewSystem("", path);
    }

    private FileSystem getOrCreateNewSystem(String keyPrefix, Path path) {
        final Map<String, Object> args = new HashMap<>();
        args.put("packagePath", path.toAbsolutePath());

        try {
            URI uri = new URI(super.getScheme() + ':' + keyPrefix + path.toUri().toString().replace('\\', '/'));
            return super.newFileSystem(uri, args);
        } catch (Exception e) {
            throw new UncheckedIOException("Failed to create intermediary FS.", new IOException("Failed to process data.", e));
        }
    }

    @Override
    public Path getPath(final URI uri)
    {
        final String[] sections = uri.getRawSchemeSpecificPart().split("~");
        if (sections.length == 1)
            return super.getPath(uri);

        FileSystem workingSystem = FileSystems.getDefault(); //Grab the normal disk FS.
        if (sections.length > 1)
        {
            for (int i = 0; i < sections.length - 1; i++)
            {
                final String section = sections[i];
                final Path path = workingSystem.getPath(section);
                workingSystem = getOrCreateNewSystem(path);
            }
        }

        final String lastSection = sections[sections.length - 1];
        return workingSystem.getPath(lastSection);
    }

    @Override
    public FileSystem getFileSystem(final URI uri)
    {
        final String[] sections = uri.getRawSchemeSpecificPart().split("~");
        if (sections.length == 1)
        {
            return super.getFileSystem(uri);
        }

        FileSystem workingSystem = FileSystems.getDefault(); //Grab the normal disk FS.
        if (sections.length > 1)
        {
            for (int i = 0; i < sections.length - 1; i++)
            {
                final String section = sections[i];
                final Path path = workingSystem.getPath(section);
                workingSystem = getOrCreateNewSystem(path);
            }
        }

        final String lastSection = sections[sections.length - 1];
        final Path lastPath = workingSystem.getPath(lastSection);
        return getOrCreateNewSystem(lastPath);
    }

    @Override
    protected URI buildUriFor(final PathPath path) throws URISyntaxException, IllegalArgumentException
    {
        String prefix = "";

        prefix = buildPrefixFor(path.getFileSystem().getTarget());

        return URI.create(String.format("%s:%s%s", SCHEME, prefix, path)
                                .replace(String.format("%s/", PATH_SEPERATOR), PATH_SEPERATOR));
    }

    protected String buildPrefixFor(final Path path) {
        if (path instanceof PathPath) {
            return buildPrefixFor(((PathPath) path).getFileSystem().getTarget()) + PATH_SEPERATOR + path.toAbsolutePath();
        }

        return path.toAbsolutePath().toUri().getRawSchemeSpecificPart();
    }

    @SuppressWarnings("resource")
    @Override
    public Path adaptResolvedPath(final PathPath path)
    {
        if (!path.toString().contains(PATH_SEPERATOR))
            return path;

        final Path workingPath = path.getFileSystem()
                                     .getPath(path.toString()
                                                  .substring(0, path.toString().lastIndexOf(PATH_SEPERATOR)) + PATH_SEPERATOR);
        final FileSystem workingSystem;
        try
        {
            workingSystem = FileSystems.newFileSystem(workingPath.toUri(), new HashMap<>());
            return workingSystem.getPath(path.endsWith(PATH_SEPERATOR) ? "/" : path.toString()
                                                                                   .substring(path.toString()
                                                                                                  .lastIndexOf(PATH_SEPERATOR) + 2));
        } catch (IOException e)
        {
            throw new IllegalArgumentException("Failed to get sub file system for path!", e);
        }
    }

    @Override
    public String[] adaptPathParts(final String longstring, final String[] pathParts)
    {
        if (!longstring.endsWith(PATH_SEPERATOR))
            return pathParts;

        pathParts[pathParts.length - 1] = pathParts[pathParts.length - 1] + "/";
        return pathParts;
    }

    private AdaptedURIWithPrefixSelection adaptUriSections(final String[] sections) {
        String keyPrefix = "";
        FileSystem workingSystem = FileSystems.getDefault();

        //First try a reverse lookup of a key based approach:
        final Optional<FileSystem> rootKnownCandidateSystem = super.getFileSystemFromKey(sections[0]);
        if (rootKnownCandidateSystem.isPresent()) {
            //Okay special case: We have a file system in the root that is known to us.
            //We will recursively resolve this until we have handled all sections:
            //First deal with the case that we do not have any other paths:
            if (sections.length == 1) {
                return new AdaptedURIWithPrefixSelection(rootKnownCandidateSystem.get(), sections[0]);
            }

            workingSystem = rootKnownCandidateSystem.get();
            keyPrefix += sections[0].replace("\\", "/") + PATH_SEPERATOR;

            for (int i = 1; i < sections.length - 2; i++)
            {
                String section = sections[i];
                if (section.startsWith("/"))
                    section = section.substring(1);

                final Path path = workingSystem.getPath(section).toAbsolutePath();
                workingSystem = getOrCreateNewSystem(keyPrefix, path);
                keyPrefix += path.toString().replace("\\", "/") + PATH_SEPERATOR;
            }

            return new AdaptedURIWithPrefixSelection(workingSystem, sections[sections.length - 1]);
        }

        //This is now a special case here, we might be in native land so we need to deal with it.
        for (int i = 0; i < sections.length - 1; i++)
        {
            String section = sections[i];
            if (section.startsWith("//"))
                section = section.substring(2);

            section = handleAbsolutePrefixOnWindows(workingSystem, section);
            final Path path = workingSystem.getPath(section).toAbsolutePath();
            workingSystem = getOrCreateNewSystem(keyPrefix, path);
            keyPrefix += path.toString().replace("\\", "/") + PATH_SEPERATOR;
        }

        return new AdaptedURIWithPrefixSelection(workingSystem, keyPrefix);
    }

    private final class AdaptedURIWithPrefixSelection {
        private final String prefix;
        private final FileSystem fileSystem;

        private AdaptedURIWithPrefixSelection(final FileSystem fileSystem, final String prefix) {
            this.prefix = prefix;
            this.fileSystem = fileSystem;
        }

        public String getPrefix() {
            return prefix;
        }

        public FileSystem getFileSystem() {
            return fileSystem;
        }
    }
}
