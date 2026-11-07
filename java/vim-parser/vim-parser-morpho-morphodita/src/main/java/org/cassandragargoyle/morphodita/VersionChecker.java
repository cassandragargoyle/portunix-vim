/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.morphodita;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Checks for newer MorphoDiTa versions on GitHub.
 * Provides informational updates only - does not auto-download.
 */
public final class VersionChecker
{
    private static final String GITHUB_API_URL =
        "https://api.github.com/repos/ufal/morphodita/releases/latest";
    private static final String GITHUB_RELEASES_URL =
        "https://github.com/ufal/morphodita/releases/tag/";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private VersionChecker()
    {
        // Utility class
    }

    /**
     * Check if newer version is available on GitHub.
     *
     * @return UpdateInfo if update available, empty if current is latest or check failed
     */
    public static Optional<UpdateInfo> checkForUpdates()
    {
        String currentVersion = MorphoDiTa.getVersion();
        return checkForUpdates(currentVersion);
    }

    /**
     * Check if newer version is available compared to specified version.
     *
     * @param currentVersion Version to compare against
     * @return UpdateInfo if update available, empty if current is latest or check failed
     */
    public static Optional<UpdateInfo> checkForUpdates(String currentVersion)
    {
        try
        {
            String latestVersion = fetchLatestVersion();
            if (latestVersion != null && isNewer(latestVersion, currentVersion))
            {
                String releaseUrl = GITHUB_RELEASES_URL + "v" + latestVersion;
                return Optional.of(new UpdateInfo(currentVersion, latestVersion, releaseUrl));
            }
            return Optional.empty();
        }
        catch (Exception e)
        {
            // Silently fail - version check is informational only
            return Optional.empty();
        }
    }

    /**
     * Fetch the latest version from GitHub API.
     *
     * @return Latest version string (without 'v' prefix), or null if fetch failed
     */
    public static String fetchLatestVersion()
    {
        try
        {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API_URL))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "MorphoDiTa-JNI-Wrapper")
                .timeout(TIMEOUT)
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200)
            {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String tagName = json.get("tag_name").getAsString();
                // Remove 'v' prefix if present
                return tagName.startsWith("v") ? tagName.substring(1) : tagName;
            }
        }
        catch (Exception e)
        {
            // Silently fail
        }
        return null;
    }

    /**
     * Compare two version strings (semantic versioning).
     *
     * @param version1 First version
     * @param version2 Second version
     * @return true if version1 is newer than version2
     */
    static boolean isNewer(String version1, String version2)
    {
        if (version1 == null || version2 == null)
        {
            return false;
        }

        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLength; i++)
        {
            int v1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int v2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (v1 > v2)
            {
                return true;
            }
            else if (v1 < v2)
            {
                return false;
            }
        }
        return false;
    }

    private static int parseVersionPart(String part)
    {
        try
        {
            // Remove non-numeric suffix (e.g., "3-beta" -> 3)
            String numericPart = part.replaceAll("[^0-9].*", "");
            return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }
}
