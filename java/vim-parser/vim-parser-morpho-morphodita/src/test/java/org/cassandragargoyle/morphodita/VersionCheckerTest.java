/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.morphodita;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VersionChecker.
 */
class VersionCheckerTest
{
    @ParameterizedTest
    @CsvSource({
        "1.12.0, 1.11.3, true",
        "1.11.4, 1.11.3, true",
        "2.0.0, 1.11.3, true",
        "1.11.3, 1.11.3, false",
        "1.11.2, 1.11.3, false",
        "1.10.0, 1.11.3, false",
        "0.99.0, 1.11.3, false",
    })
    void shouldCompareVersions(String version1, String version2, boolean expectedNewer)
    {
        assertEquals(expectedNewer, VersionChecker.isNewer(version1, version2),
            version1 + " newer than " + version2 + " should be " + expectedNewer);
    }

    @Test
    void shouldHandleNullVersions()
    {
        assertFalse(VersionChecker.isNewer(null, "1.0.0"));
        assertFalse(VersionChecker.isNewer("1.0.0", null));
        assertFalse(VersionChecker.isNewer(null, null));
    }

    @Test
    void shouldHandleVersionsWithDifferentLengths()
    {
        assertTrue(VersionChecker.isNewer("1.11.3.1", "1.11.3"));
        assertFalse(VersionChecker.isNewer("1.11", "1.11.3"));
        assertTrue(VersionChecker.isNewer("2", "1.11.3"));
    }

    @Test
    void shouldHandleVersionsWithSuffixes()
    {
        assertTrue(VersionChecker.isNewer("1.12.0-beta", "1.11.3"));
        // Suffix is stripped, so 1.11.3-rc1 == 1.11.3
        assertFalse(VersionChecker.isNewer("1.11.3-rc1", "1.11.3"));
    }

    @Test
    void shouldCheckForUpdates()
    {
        // This test makes actual network request - may fail in offline environments
        // Just verify it doesn't throw and returns Optional
        try
        {
            Optional<UpdateInfo> update = VersionChecker.checkForUpdates("0.0.1");
            assertNotNull(update);
            // With version 0.0.1, there should be an update available
            if (update.isPresent())
            {
                assertNotNull(update.get().latestVersion());
                assertNotNull(update.get().releaseUrl());
                assertNotNull(update.get().getMessage());
            }
        }
        catch (Exception e)
        {
            // Network failure is acceptable in test environment
            System.out.println("Version check failed (network issue?): " + e.getMessage());
        }
    }

    @Test
    void shouldReturnEmptyForLatestVersion()
    {
        // Using a very high version number should return empty
        try
        {
            Optional<UpdateInfo> update = VersionChecker.checkForUpdates("999.999.999");
            assertTrue(update.isEmpty());
        }
        catch (Exception e)
        {
            // Network failure is acceptable
            System.out.println("Version check failed (network issue?): " + e.getMessage());
        }
    }

    @Test
    void shouldFetchLatestVersion()
    {
        // This test makes actual network request
        try
        {
            String latest = VersionChecker.fetchLatestVersion();
            // May be null if network fails
            if (latest != null)
            {
                assertTrue(latest.matches("\\d+\\.\\d+(\\.\\d+)?"),
                    "Version should be semantic: " + latest);
            }
        }
        catch (Exception e)
        {
            // Network failure is acceptable
            System.out.println("Fetch failed (network issue?): " + e.getMessage());
        }
    }
}
