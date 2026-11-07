/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.morphodita.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Handles loading of MorphoDiTa native libraries from JAR resources.
 * Extracts platform-specific native library to a temp directory and loads it.
 */
public final class NativeLoader
{
    private static final String RESOURCE_PREFIX = "/native/";
    private static volatile boolean loaded = false;
    private static volatile boolean available = false;
    private static volatile String loadError = null;

    private NativeLoader()
    {
        // Utility class
    }

    /**
     * Load MorphoDiTa native library. Safe to call multiple times.
     *
     * @return true if native library loaded successfully
     */
    public static synchronized boolean load()
    {
        if (loaded)
        {
            return available;
        }

        try
        {
            String platform = detectPlatform();
            Path tempDir = Files.createTempDirectory("morphodita-jni-");

            // Register shutdown hook for cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try
                {
                    deleteRecursively(tempDir);
                }
                catch (IOException ignored)
                {
                    // Best effort cleanup
                }
            }));

            // Extract and load JNI bridge library
            extractAndLoad(platform, getLibraryName(platform), tempDir);

            available = true;
            loaded = true;
            return true;
        }
        catch (UnsatisfiedLinkError | IOException e)
        {
            loadError = e.getMessage();
            loaded = true;
            available = false;
            return false;
        }
    }

    /**
     * Check if MorphoDiTa native library is available without loading it.
     *
     * @return true if native library can be loaded
     */
    public static boolean isAvailable()
    {
        if (!loaded)
        {
            load();
        }
        return available;
    }

    /**
     * Get the error message if loading failed.
     *
     * @return Error message or null if no error
     */
    public static String getLoadError()
    {
        return loadError;
    }

    /**
     * Get the detected platform identifier.
     *
     * @return Platform string (e.g., "linux-x86_64")
     */
    public static String getPlatform()
    {
        return detectPlatform();
    }

    private static void extractAndLoad(String platform, String libName, Path tempDir) throws IOException
    {
        String resourcePath = RESOURCE_PREFIX + platform + "/" + libName;

        try (InputStream in = NativeLoader.class.getResourceAsStream(resourcePath))
        {
            if (in == null)
            {
                throw new UnsatisfiedLinkError("Native library not found in JAR: " + resourcePath +
                    ". Run scripts/fetch-natives.sh to download pre-built binaries.");
            }

            Path outPath = tempDir.resolve(libName);
            Files.copy(in, outPath, StandardCopyOption.REPLACE_EXISTING);
            outPath.toFile().deleteOnExit();

            System.load(outPath.toAbsolutePath().toString());
        }
    }

    private static String detectPlatform()
    {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        if (os.contains("linux") && (arch.contains("amd64") || arch.contains("x86_64")))
        {
            return "linux-x86_64";
        }
        else if (os.contains("windows") && (arch.contains("amd64") || arch.contains("x86_64")))
        {
            return "windows-x86_64";
        }

        throw new UnsatisfiedLinkError("Unsupported platform: " + os + "/" + arch +
            ". Supported: linux-x86_64, windows-x86_64");
    }

    private static String getLibraryName(String platform)
    {
        if (platform.startsWith("windows"))
        {
            return "morphodita_java.dll";
        }
        else
        {
            return "libmorphodita_java.so";
        }
    }

    private static void deleteRecursively(Path path) throws IOException
    {
        if (Files.isDirectory(path))
        {
            try (var stream = Files.list(path))
            {
                for (Path child : stream.toList())
                {
                    deleteRecursively(child);
                }
            }
        }
        Files.deleteIfExists(path);
    }
}
