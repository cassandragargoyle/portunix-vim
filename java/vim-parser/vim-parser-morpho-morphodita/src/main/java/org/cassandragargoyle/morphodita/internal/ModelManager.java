/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.morphodita.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Manages MorphoDiTa language models.
 * Models are installed via Portunix to a shared data directory.
 */
public final class ModelManager
{
    private static final String MODELS_SUBDIR = "models/morphodita";
    private static final String MODEL_EXTENSION = ".tagger";

    private final Path modelsDirectory;

    /**
     * Create model manager for default Portunix data directory.
     */
    public ModelManager()
    {
        this(resolveDefaultModelsDirectory());
    }

    /**
     * Create model manager for specified directory.
     *
     * @param modelsDirectory Directory containing model files
     */
    public ModelManager(Path modelsDirectory)
    {
        this.modelsDirectory = modelsDirectory;
    }

    /**
     * Find model file for specified language.
     *
     * @param language Language code (e.g., "czech", "english")
     * @return Path to model file, or empty if not found
     */
    public Optional<Path> findModel(String language)
    {
        if (!Files.isDirectory(modelsDirectory))
        {
            return Optional.empty();
        }

        String normalizedLang = language.toLowerCase().trim();

        // Try exact match first
        Path exactMatch = modelsDirectory.resolve(normalizedLang + MODEL_EXTENSION);
        if (Files.isRegularFile(exactMatch))
        {
            return Optional.of(exactMatch);
        }

        // Try pattern matching (e.g., "czech-morfflex2-pdtc.tagger")
        try (var stream = Files.list(modelsDirectory))
        {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(MODEL_EXTENSION))
                .filter(p -> p.getFileName().toString().toLowerCase().startsWith(normalizedLang))
                .findFirst();
        }
        catch (Exception e)
        {
            return Optional.empty();
        }
    }

    /**
     * List all available models.
     *
     * @return List of model file paths
     */
    public List<Path> listModels()
    {
        if (!Files.isDirectory(modelsDirectory))
        {
            return Collections.emptyList();
        }

        try (var stream = Files.list(modelsDirectory))
        {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(MODEL_EXTENSION))
                .toList();
        }
        catch (Exception e)
        {
            return Collections.emptyList();
        }
    }

    /**
     * List available model names (languages).
     *
     * @return List of model names derived from file names
     */
    public List<String> listModelNames()
    {
        List<String> names = new ArrayList<>();
        for (Path model : listModels())
        {
            String fileName = model.getFileName().toString();
            // Remove extension and extract language prefix
            String name = fileName.substring(0, fileName.length() - MODEL_EXTENSION.length());
            // Extract language (first part before dash)
            int dashIndex = name.indexOf('-');
            if (dashIndex > 0)
            {
                names.add(name.substring(0, dashIndex));
            }
            else
            {
                names.add(name);
            }
        }
        return names;
    }

    /**
     * Get the models directory path.
     *
     * @return Path to models directory
     */
    public Path getModelsDirectory()
    {
        return modelsDirectory;
    }

    /**
     * Check if models directory exists.
     *
     * @return true if directory exists
     */
    public boolean isAvailable()
    {
        return Files.isDirectory(modelsDirectory);
    }

    private static Path resolveDefaultModelsDirectory()
    {
        // Check PORTUNIX_DATA environment variable first
        String portunixData = System.getenv("PORTUNIX_DATA");
        if (portunixData != null && !portunixData.isBlank())
        {
            return Path.of(portunixData, MODELS_SUBDIR);
        }

        // Fallback to user home directory
        String userHome = System.getProperty("user.home");
        return Path.of(userHome, ".portunix", MODELS_SUBDIR);
    }
}
