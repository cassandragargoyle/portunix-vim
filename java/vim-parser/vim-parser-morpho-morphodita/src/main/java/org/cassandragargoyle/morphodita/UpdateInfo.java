/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.morphodita;

/**
 * Information about available MorphoDiTa update.
 *
 * @param currentVersion Currently installed version
 * @param latestVersion Latest available version on GitHub
 * @param releaseUrl URL to the GitHub release page
 */
public record UpdateInfo(
    String currentVersion,
    String latestVersion,
    String releaseUrl
) {
    /**
     * Get human-readable message about the update.
     *
     * @return Update notification message
     */
    public String getMessage()
    {
        return String.format(
            "MorphoDiTa update available: %s -> %s%nDownload: %s",
            currentVersion, latestVersion, releaseUrl
        );
    }
}
