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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ZipFsPathFsCompatTests {

    @Test
    public void relativizeDirectoryTest() throws URISyntaxException, IOException
    {
        final Path windowsPath = Paths.get("src/binks/resources/dir1.zip");
        FileSystem jarFs = FileSystems.newFileSystem(URI.create("jar:" + windowsPath.toUri()), new HashMap<>());
        FileSystem pathFs = FileSystems.newFileSystem(URI.create("path:///test"), createMap("packagePath", Paths.get("src/binks/resources/dir1.zip")));

        assertEquals(
                jarFs.getPath("/abc/xyz").relativize(jarFs.getPath("/abc/def/ghi")).toString(),
                pathFs.getPath("/abc/xyz").relativize(pathFs.getPath("/abc/def/ghi")).toString()
        );

        jarFs.close();
        pathFs.close();
    }

    @Test
    public void nameCountTest() throws URISyntaxException, IOException
    {
        final Path windowsPath = Paths.get("src/binks/resources/dir1.zip");
        FileSystem jarFs = FileSystems.newFileSystem(URI.create("jar:" + windowsPath.toUri()), new HashMap<>());
        FileSystem pathFs = FileSystems.newFileSystem(URI.create("path:///test"), createMap("packagePath", Paths.get("src/binks/resources/dir1.zip")));

        assertEquals(
                jarFs.getPath("/abc/xyz").getNameCount(),
                pathFs.getPath("/abc/xyz").getNameCount()
        );

        jarFs.close();
        pathFs.close();
    }

    @Test
    public void rootsOfNormalZipFSIsDifferentDueToFML() throws URISyntaxException, IOException
    {
        final Path windowsPath = Paths.get("src/binks/resources/dir1.zip");
        FileSystem jarFs = FileSystems.newFileSystem(URI.create("jar:" + windowsPath.toUri()), new HashMap<>());
        FileSystem pathFs = FileSystems.newFileSystem(URI.create("path:///test"), createMap("packagePath", Paths.get("src/binks/resources/dir1.zip")));

        //NOTE: This should normally be equals but in this particular case we do not want the FS spec for the ZIP since it
        // fails in the case of the root for the use of PathFS in FML when it detects filenames.
        assertNotEquals(
                jarFs.getPath("/abc/xyz").getRoot().toString(),
                pathFs.getPath("/abc/xyz").getRoot().toString()
        );

        jarFs.close();
        pathFs.close();
    }

    @Test
    public void parentTest() throws URISyntaxException, IOException
    {
        final Path windowsPath = Paths.get("src/binks/resources/dir1.zip");
        FileSystem jarFs = FileSystems.newFileSystem(URI.create("jar:" + windowsPath.toUri()), new HashMap<>());
        FileSystem pathFs = FileSystems.newFileSystem(URI.create("path:///test"), createMap("packagePath", windowsPath));

        assertEquals(
                jarFs.getPath("").getParent(),
                pathFs.getPath("").getParent()
        );

        jarFs.close();
        pathFs.close();
    }

    @Test
    public void existsTest() throws URISyntaxException, IOException
    {
        final Path windowsPath = Paths.get("src/binks/resources/dir1.zip");
        FileSystem jarFs = FileSystems.newFileSystem(URI.create("jar:" + windowsPath.toUri()), new HashMap<>());
        FileSystem pathFs = FileSystems.newFileSystem(URI.create("path:///test"), createMap("packagePath", windowsPath));

        Path rootInJarFs = jarFs.getPath("/");
        Path rootInPathFs = pathFs.getPath("/");

        Path existingInJarFs = jarFs.getPath("masktest.txt");
        Path existingInPathFs = pathFs.getPath("masktest.txt");

        Path notExistingInJarFs = jarFs.getPath("masktest.txt2.doesnotexist");
        Path notExistingInPathFs = pathFs.getPath("masktest.txt2.doesnotexist");

        assertEquals(
                Files.exists(rootInJarFs),
                Files.exists(rootInPathFs)
        );

        assertEquals(
                Files.exists(existingInJarFs),
                Files.exists(existingInPathFs)
        );

        assertEquals(
                Files.exists(notExistingInJarFs),
                Files.exists(notExistingInPathFs)
        );

        jarFs.close();
        pathFs.close();
    }

    private static Map<String, Object> createMap(final String key, final Object o) {
        final HashMap<String, Object> map = new HashMap<>();
        map.put(key, o);
        return map;
    }
}
