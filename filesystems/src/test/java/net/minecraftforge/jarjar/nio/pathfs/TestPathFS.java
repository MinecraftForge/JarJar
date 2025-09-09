/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.nio.pathfs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("resource")
public class TestPathFS
{
    @Test
    public void redirectionTest() throws URISyntaxException, IOException
    {
        final Path target =  Paths.get("src", "test", "resources", "dir1.zip");

        final URI pathFsUri = new URI("path:///test");
        final Map<String, ?> args = createMap("packagePath", target);

        final FileSystem pathFS = FileSystems.newFileSystem(pathFsUri, args);

        final Path pathFsPath = pathFS.getPath("/");

        final byte[] pathFsData = Files.readAllBytes(pathFsPath);
        final byte[] sourceData = Files.readAllBytes(target);

        assertArrayEquals(sourceData, pathFsData);
    }

    @Test
    public void relativeDirectoryMapTest() throws URISyntaxException, IOException
    {
        final Path target =  Paths.get("src", "test");

        final URI pathFsUri = new URI("path://test");
        final Map<String, ?> args = createMap("packagePath", target);

        final FileSystem pathFS = FileSystems.newFileSystem(pathFsUri, args);

        final List<String> pathFSDirectories = Files.walk(pathFS.getPath("")).filter(Files::isDirectory).map(Path::getFileName).map(Path::toString).collect(Collectors.toList());
        final List<String> sourceDirectories = Files.walk(target).filter(Files::isDirectory).map(target::relativize).map(Path::getFileName).map(Path::toString).collect(Collectors.toList());

        //We need to remove the root of the tree.
        //This is caused by the filename behaviour of PathFS where it returns the target file name on the root.
        pathFSDirectories.remove(0);
        sourceDirectories.remove(0);

        assertIterableEquals(sourceDirectories, pathFSDirectories);
    }

    @Test
    public void absoluteDirectoryMapTest() throws URISyntaxException, IOException
    {
        final Path target =  Paths.get("src", "test");

        final URI pathFsUri = new URI("path://test");
        final Map<String, ?> args = createMap("packagePath", target);

        final FileSystem pathFS = FileSystems.newFileSystem(pathFsUri, args);

        final List<String> pathFSDirectories = Files.walk(pathFS.getPath("/")).filter(Files::isDirectory).map(Path::getFileName).map(Path::toString).collect(Collectors.toList());
        final List<String> sourceDirectories = Files.walk(target).filter(Files::isDirectory).map(target::relativize).map(Path::getFileName).map(Path::toString).collect(Collectors.toList());

        //We need to remove the root of the tree.
        //This is caused by the filename behaviour of PathFS where it returns the target file name on the root.
        pathFSDirectories.remove(0);
        sourceDirectories.remove(0);

        assertIterableEquals(sourceDirectories, pathFSDirectories);
    }

    @Test
    public void relativePathToRelativeUriTest() throws URISyntaxException, IOException
    {
        final Path target =  Paths.get("src", "test");

        final URI pathFsUri = new URI("path://test");
        final Map<String, ?> args = createMap("packagePath", target);

        final FileSystem pathFS = FileSystems.newFileSystem(pathFsUri, args);

        final Path pathInPathFS = pathFS.getPath("dir1.zip");
        final URI uriInPathFS = pathInPathFS.toUri();

        assertNotNull(uriInPathFS);
        assertEquals(pathFS.provider().getScheme(), uriInPathFS.getScheme());
        assertEquals("test~/dir1.zip", uriInPathFS.getRawSchemeSpecificPart());
    }

    @Test
    public void relativeUriToRelativePathTest() throws URISyntaxException, IOException
    {
        final Path target =  Paths.get("src", "test");

        final URI pathFsUri = new URI("path://test");
        final Map<String, ?> args = createMap("packagePath", target);

        //This needs to be run since else the test FS does not exist!
        final FileSystem pathFS = FileSystems.newFileSystem(pathFsUri, args);

        final URI uriInPathFS = new URI("path:test~/dir1.zip");
        final Path pathInPathFS = Paths.get(uriInPathFS);

        assertNotNull(pathInPathFS);
        assertTrue(pathInPathFS instanceof PathPath);
        assertEquals("dir1.zip",pathInPathFS.toString());
    }

    @Test
    public void absolutePathToAbsoluteUriTest() throws URISyntaxException, IOException
    {
        final Path target =  Paths.get("src", "test");

        final URI pathFsUri = new URI("path://test");
        final Map<String, ?> args = createMap("packagePath", target);

        final FileSystem pathFS = FileSystems.newFileSystem(pathFsUri, args);

        final Path pathInPathFS = pathFS.getPath("/dir1.zip");
        final URI uriInPathFS = pathInPathFS.toUri();

        assertNotNull(uriInPathFS);
        assertEquals(pathFS.provider().getScheme(), uriInPathFS.getScheme());
        assertEquals("test~/dir1.zip", uriInPathFS.getRawSchemeSpecificPart());
    }

    @Test
    public void absoluteUriToAbsolutePathTest() throws URISyntaxException, IOException
    {
        final Path target =  Paths.get("src", "test");

        final URI pathFsUri = new URI("path://test");
        final Map<String, ?> args = createMap("packagePath", target);

        final FileSystem pathFS = FileSystems.newFileSystem(pathFsUri, args);

        final URI uriInPathFS = new URI("path:test~/dir1.zip");
        final Path pathInPathFS = Paths.get(uriInPathFS);

        assertNotNull(pathInPathFS);
        assertTrue(pathInPathFS instanceof PathPath);
        assertEquals("dir1.zip", pathInPathFS.toString());
    }

    @Test
    public void relativePathToRelativeUriToRelativePathTest() throws URISyntaxException, IOException
    {
        final Path target =  Paths.get("src", "test");

        final URI pathFsUri = new URI("path://test");
        final Map<String, ?> args = createMap("packagePath", target);

        final FileSystem pathFS = FileSystems.newFileSystem(pathFsUri, args);

        final Path pathInPathFS = pathFS.getPath("dir1.zip");
        final URI uriInPathFS = pathInPathFS.toUri();
        final Path resultPath = Paths.get(uriInPathFS);

        assertEquals(pathInPathFS, resultPath);
    }

    @Test
    public void relativeUriToRelativePathToRelativeUriTest() throws URISyntaxException, IOException
    {
        final Path target =  Paths.get("src", "test");

        final URI pathFsUri = new URI("path://test");
        final Map<String, ?> args = createMap("packagePath", target);

        //This needs to be run since else the test FS does not exist!
        final FileSystem pathFS = FileSystems.newFileSystem(pathFsUri, args);

        final URI uriInPathFS = new URI("path:test~/dir1.zip");
        final Path pathInPathFS = Paths.get(uriInPathFS);
        final URI resultUri = pathInPathFS.toUri();

        assertEquals(uriInPathFS, resultUri);
    }

    @Test
    public void recursiveRelativeRedirectionTest() throws URISyntaxException, IOException
    {
        final Path target =  Paths.get("src", "test", "resources");

        final URI outerPathFsUri = new URI("path://outer");
        final Map<String, ?> outerFsArgs = createMap("packagePath", target);

        final FileSystem outerPathFS = FileSystems.newFileSystem(outerPathFsUri, outerFsArgs);
        final Path pathInOuterFS = outerPathFS.getPath("dir1.zip");

        final URI innerPathFsUri = new URI("path://inner");
        final Map<String, ?> innerFsArgs = createMap("packagePath", pathInOuterFS);

        final FileSystem innerPathFS = FileSystems.newFileSystem(innerPathFsUri, innerFsArgs);
        final byte[] pathFsData = Files.readAllBytes(innerPathFS.getPath(""));
        final byte[] sourceData = Files.readAllBytes(target.resolve("dir1.zip"));

        assertArrayEquals(sourceData, pathFsData);
    }

    @Test
    public void absoluteRelativeRedirectionTest() throws URISyntaxException, IOException
    {
        final Path target =  Paths.get("src", "test", "resources");

        final URI outerPathFsUri = new URI("path://outer");
        final Map<String, ?> outerFsArgs = createMap("packagePath", target);

        final FileSystem outerPathFS = FileSystems.newFileSystem(outerPathFsUri, outerFsArgs);
        final Path pathInOuterFS = outerPathFS.getPath("/dir1.zip");

        final URI innerPathFsUri = new URI("path://inner");
        final Map<String, ?> innerFsArgs = createMap("packagePath", pathInOuterFS);

        final FileSystem innerPathFS = FileSystems.newFileSystem(innerPathFsUri, innerFsArgs);
        final byte[] pathFsData = Files.readAllBytes(innerPathFS.getPath("/"));
        final byte[] sourceData = Files.readAllBytes(target.resolve("dir1.zip"));

        assertArrayEquals(sourceData, pathFsData);
    }

    @Test
    public void recursiveRelativePathToRelativeUriTest() throws URISyntaxException, IOException
    {
        final Path target =  Paths.get("src", "test", "resources");

        final URI outerPathFsUri = new URI("path://outer");
        final Map<String, ?> outerFsArgs = createMap("packagePath", target);

        final FileSystem outerPathFS = FileSystems.newFileSystem(outerPathFsUri, outerFsArgs);
        final Path pathInOuterFS = outerPathFS.getPath("dir1.zip");

        final URI innerPathFsUri = new URI("path://inner");
        final Map<String, ?> innerFsArgs = createMap("packagePath", pathInOuterFS);

        final FileSystem innerPathFS = FileSystems.newFileSystem(innerPathFsUri, innerFsArgs);
        final Path pathInPathFS = innerPathFS.getPath("subdir1");
        final URI uriInPathFS = pathInPathFS.toUri();

        assertNotNull(uriInPathFS);
        assertEquals(innerPathFS.provider().getScheme(), uriInPathFS.getScheme());
        assertEquals("inner~/subdir1", uriInPathFS.getRawSchemeSpecificPart());
    }

    @Test
    public void recursiveRelativeUriToRelativePathTest() throws URISyntaxException, IOException
    {
        final Path target =  Paths.get("src", "test", "resources");

        final URI outerPathFsUri = new URI("path://outer");
        final Map<String, ?> outerFsArgs = createMap("packagePath", target);

        final FileSystem outerPathFS = FileSystems.newFileSystem(outerPathFsUri, outerFsArgs);
        final Path pathInOuterFS = outerPathFS.getPath("dir1.zip");

        final URI innerPathFsUri = new URI("path://inner");
        final Map<String, ?> innerFsArgs = createMap("packagePath", pathInOuterFS);

        //Needs to be created so that the name for the inner fs is known.
        final FileSystem innerPathFS = FileSystems.newFileSystem(innerPathFsUri, innerFsArgs);

        final URI uriInPathFS = new URI("path:outer~subdir1");
        final Path pathInPathFS = Paths.get(uriInPathFS);

        assertNotNull(pathInPathFS);
        assertTrue(pathInPathFS instanceof PathPath);
        assertEquals("subdir1",pathInPathFS.toString());
    }

    @Test
    public void recursiveAbsolutePathToAbsoluteUriTest() throws URISyntaxException, IOException
    {
        final Path target =  Paths.get("src", "test", "resources");

        final URI outerPathFsUri = new URI("path://outer");
        final Map<String, ?> outerFsArgs = createMap("packagePath", target);

        final FileSystem outerPathFS = FileSystems.newFileSystem(outerPathFsUri, outerFsArgs);
        final Path pathInOuterFS = outerPathFS.getPath("/dir1.zip");

        final URI innerPathFsUri = new URI("path://inner");
        final Map<String, ?> innerFsArgs = createMap("packagePath", pathInOuterFS);

        final FileSystem innerPathFS = FileSystems.newFileSystem(innerPathFsUri, innerFsArgs);
        final Path pathInPathFS = innerPathFS.getPath("/subdir1");
        final URI uriInPathFS = pathInPathFS.toUri();

        assertNotNull(uriInPathFS);
        assertEquals(innerPathFS.provider().getScheme(), uriInPathFS.getScheme());
        assertEquals("inner~/subdir1", uriInPathFS.getRawSchemeSpecificPart());
    }

    @Test
    public void recursiveAbsoluteUriToAbsolutePathTest() throws URISyntaxException, IOException
    {
        final Path target =  Paths.get("src", "test", "resources");

        final URI outerPathFsUri = new URI("path://outer");
        final Map<String, ?> outerFsArgs = createMap("packagePath", target);

        final FileSystem outerPathFS = FileSystems.newFileSystem(outerPathFsUri, outerFsArgs);
        final Path pathInOuterFS = outerPathFS.getPath("/dir1.zip");

        final URI innerPathFsUri = new URI("path://inner");
        final Map<String, ?> innerFsArgs = createMap("packagePath", pathInOuterFS);

        //Needs to be created so that the name for the inner fs is known.
        final FileSystem innerPathFS = FileSystems.newFileSystem(innerPathFsUri, innerFsArgs);

        final URI uriInPathFS = new URI("path:outer~/subdir1");
        final Path pathInPathFS = Paths.get(uriInPathFS);

        assertNotNull(pathInPathFS);
        assertTrue(pathInPathFS instanceof PathPath);
        assertEquals("subdir1",pathInPathFS.toString());
    }

    private static Map<String, Object> createMap(final String key, final Object o) {
        final HashMap<String, Object> map = new HashMap<>();
        map.put(key, o);
        return map;
    }

}
