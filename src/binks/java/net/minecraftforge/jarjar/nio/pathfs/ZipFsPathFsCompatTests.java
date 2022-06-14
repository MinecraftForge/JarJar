package net.minecraftforge.jarjar.nio.pathfs;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("resource")
public class ZipFsPathFsCompatTests {

    @Test
    public void relativizeDirectoryTest() throws URISyntaxException, IOException
    {
        final Path windowsPath = Paths.get("src/binks/resources/dir1.zip");
        FileSystem jarFs = FileSystems.newFileSystem(URI.create("jar:" + windowsPath.toUri()), new HashMap<>());
        FileSystem pathFs = FileSystems.newFileSystem(URI.create("path:///test"), ImmutableMap.of("packagePath", Paths.get("src/binks/resources/dir1.zip")));

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
        FileSystem pathFs = FileSystems.newFileSystem(URI.create("path:///test"), ImmutableMap.of("packagePath", Paths.get("src/binks/resources/dir1.zip")));

        assertEquals(
                jarFs.getPath("/abc/xyz").getNameCount(),
                pathFs.getPath("/abc/xyz").getNameCount()
        );

        jarFs.close();
        pathFs.close();
    }

    @Test
    public void rootTest() throws URISyntaxException, IOException
    {
        final Path windowsPath = Paths.get("src/binks/resources/dir1.zip");
        FileSystem jarFs = FileSystems.newFileSystem(URI.create("jar:" + windowsPath.toUri()), new HashMap<>());
        FileSystem pathFs = FileSystems.newFileSystem(URI.create("path:///test"), ImmutableMap.of("packagePath", Paths.get("src/binks/resources/dir1.zip")));

        assertEquals(
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
        FileSystem pathFs = FileSystems.newFileSystem(URI.create("path:///test"), ImmutableMap.of("packagePath", windowsPath));

        assertEquals(
                jarFs.getPath("").getParent(),
                pathFs.getPath("").getParent()
        );

        jarFs.close();
        pathFs.close();
    }
}
