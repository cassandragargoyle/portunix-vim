/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.tk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Accuracy test for Czech stemming using TK Stemmer
 * Test data is loaded from czech-stemming-test-data.json
 *
 * All tests are conditionally enabled based on native library availability
 */
class CzechStemmingAccuracyTest
{
    private static final String TEST_DATA_FILE = "/czech-stemming-test-data.json";

    private static JsonNode testData;
    private static String language;
    private static double nounThreshold;
    private static double adjectiveThreshold;
    private static double overallThreshold;
    private static int minTokensPerSecond;

    private TKStemmer stemmer;

    @BeforeAll
    static void loadTestData() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = CzechStemmingAccuracyTest.class.getResourceAsStream(TEST_DATA_FILE))
        {
            assertNotNull(is, "Test data file not found: " + TEST_DATA_FILE);
            testData = mapper.readTree(is);
        }

        language = testData.get("language").asText();
        JsonNode thresholds = testData.get("thresholds");
        nounThreshold = thresholds.get("nounConsistency").asDouble();
        adjectiveThreshold = thresholds.get("adjectiveConsistency").asDouble();
        overallThreshold = thresholds.get("overallAccuracy").asDouble();
        minTokensPerSecond = thresholds.get("minTokensPerSecond").asInt();
    }

    @BeforeEach
    void setUp()
    {
        if (TKStemmer.isAvailable())
        {
            stemmer = TKStemmer.create(language);
        }
    }

    @AfterEach
    void tearDown()
    {
        if (stemmer != null)
        {
            stemmer.close();
        }
    }

    @Test
    @EnabledIf("isNativeAvailable")
    void testNounDeclensionConsistency()
    {
        JsonNode nouns = testData.get("nouns");
        int totalGroups = nouns.size();
        int consistentGroups = 0;

        System.out.println("\n=== TK: Czech Noun Declension Stemming Test ===\n");

        for (JsonNode noun : nouns)
        {
            String lemma = noun.get("lemma").asText();
            List<String> forms = jsonArrayToList(noun.get("forms"));

            Map<String, List<String>> stemToWords = groupByStem(forms);
            boolean isConsistent = stemToWords.size() == 1;

            if (isConsistent)
            {
                consistentGroups++;
            }

            System.out.printf("%-15s: %s -> %s%n",
                lemma,
                isConsistent ? "CONSISTENT" : "INCONSISTENT",
                stemToWords.keySet());
        }

        double accuracy = (double) consistentGroups / totalGroups;
        System.out.printf("%n=== Noun Declension Consistency: %.1f%% (%d/%d groups) ===%n",
            accuracy * 100, consistentGroups, totalGroups);

        assertTrue(accuracy >= nounThreshold,
            String.format("Noun declension consistency %.1f%% is below minimum %.1f%%",
                accuracy * 100, nounThreshold * 100));
    }

    @Test
    @EnabledIf("isNativeAvailable")
    void testAdjectiveConsistency()
    {
        JsonNode adjectives = testData.get("adjectives");
        int totalGroups = adjectives.size();
        int consistentGroups = 0;

        System.out.println("\n=== TK: Czech Adjective Declension Stemming Test ===\n");

        for (JsonNode adj : adjectives)
        {
            String lemma = adj.get("lemma").asText();
            List<String> forms = jsonArrayToList(adj.get("forms"));

            Map<String, List<String>> stemToWords = groupByStem(forms);
            boolean isConsistent = stemToWords.size() == 1;

            if (isConsistent)
            {
                consistentGroups++;
            }

            System.out.printf("%-15s: %s -> %s%n",
                lemma,
                isConsistent ? "CONSISTENT" : "INCONSISTENT",
                stemToWords.keySet());
        }

        double accuracy = (double) consistentGroups / totalGroups;
        System.out.printf("%n=== Adjective Declension Consistency: %.1f%% (%d/%d groups) ===%n",
            accuracy * 100, consistentGroups, totalGroups);

        assertTrue(accuracy >= adjectiveThreshold,
            String.format("Adjective declension consistency %.1f%% is below minimum %.1f%%",
                accuracy * 100, adjectiveThreshold * 100));
    }

    @Test
    @EnabledIf("isNativeAvailable")
    void testOverallNounStemmingAccuracy()
    {
        JsonNode nouns = testData.get("nouns");
        int totalWords = 0;
        int wordsWithMajorityStem = 0;

        System.out.println("\n=== TK: Overall Noun Stemming Accuracy Test ===\n");

        for (JsonNode noun : nouns)
        {
            List<String> forms = jsonArrayToList(noun.get("forms"));
            Map<String, Integer> stemCounts = new HashMap<>();

            for (String word : forms)
            {
                String stem = stemmer.stem(word);
                if (stem != null && !stem.isEmpty())
                {
                    stemCounts.merge(stem, 1, Integer::sum);
                }
                totalWords++;
            }

            int maxCount = stemCounts.values().stream().max(Integer::compareTo).orElse(0);
            wordsWithMajorityStem += maxCount;

            boolean allMatch = maxCount == forms.size();
            System.out.printf("%s -> stems: %s %s%n",
                forms.subList(0, Math.min(4, forms.size())),
                stemCounts,
                allMatch ? "OK" : "PARTIAL");
        }

        double accuracy = (double) wordsWithMajorityStem / totalWords;
        System.out.printf("%n=== Overall Noun Stemming Accuracy: %.1f%% (%d/%d words) ===%n",
            accuracy * 100, wordsWithMajorityStem, totalWords);

        assertTrue(accuracy >= overallThreshold,
            String.format("Overall noun stemming accuracy %.1f%% is below minimum %.1f%%",
                accuracy * 100, overallThreshold * 100));
    }

    @Test
    @EnabledIf("isNativeAvailable")
    void testProcessingSpeed()
    {
        List<String> words = jsonArrayToList(testData.get("speedTestWords"));
        int repeat = testData.get("speedTestRepeat").asInt();

        List<String> testWords = new ArrayList<>();
        for (int i = 0; i < repeat; i++)
        {
            testWords.add(words.get(i % words.size()));
        }

        // Warm up
        for (String word : testWords.subList(0, Math.min(100, testWords.size())))
        {
            stemmer.stem(word);
        }

        // Measure
        long startTime = System.nanoTime();
        int tokenCount = 0;
        for (String word : testWords)
        {
            String stem = stemmer.stem(word);
            if (stem != null)
            {
                tokenCount++;
            }
        }
        long endTime = System.nanoTime();

        double elapsedSeconds = (endTime - startTime) / 1_000_000_000.0;
        double tokensPerSecond = tokenCount / elapsedSeconds;

        System.out.printf("%n=== TK: Processing Speed Test ===%n");
        System.out.printf("Tokens processed: %d%n", tokenCount);
        System.out.printf("Time: %.3f seconds%n", elapsedSeconds);
        System.out.printf("Speed: %.0f tokens/second%n", tokensPerSecond);

        assertTrue(tokensPerSecond > minTokensPerSecond,
            String.format("Processing speed %.0f tokens/sec is below required %d tokens/sec",
                tokensPerSecond, minTokensPerSecond));
    }

    @Test
    @EnabledIf("isNativeAvailable")
    void testBatchStemming()
    {
        List<String> words = jsonArrayToList(testData.get("speedTestWords"));

        List<String> stems = stemmer.stemBatch(words);

        System.out.printf("%n=== TK: Batch Stemming Test ===%n");
        System.out.printf("Input words: %d%n", words.size());
        System.out.printf("Output stems: %d%n", stems.size());

        assertEquals(words.size(), stems.size(), "Batch stemming should return same number of stems");

        for (int i = 0; i < words.size(); i++)
        {
            String singleStem = stemmer.stem(words.get(i));
            assertEquals(singleStem, stems.get(i),
                String.format("Batch stem for '%s' should match single stem", words.get(i)));
        }
    }

    @Test
    @EnabledIf("isNativeAvailable")
    void testSampleTextProcessing()
    {
        JsonNode samples = testData.get("sampleTexts");

        for (JsonNode sample : samples)
        {
            String name = sample.get("name").asText();
            String text = sample.get("text").asText();
            int expectedMinTokens = sample.get("expectedMinTokens").asInt();
            List<String> expectedPrefixes = jsonArrayToList(sample.get("expectedStemPrefixes"));

            // Simple tokenization (split on whitespace and punctuation)
            String[] words = text.split("[\\s.,;:!?()\\[\\]{}\"']+");
            List<String> stems = new ArrayList<>();

            for (String word : words)
            {
                if (!word.isEmpty() && word.matches("\\p{L}+"))
                {
                    String stem = stemmer.stem(word.toLowerCase());
                    if (stem != null && !stem.isEmpty())
                    {
                        stems.add(stem);
                    }
                }
            }

            System.out.printf("%n=== TK: Sample Text: %s ===%n", name);
            System.out.printf("Input length: %d chars%n", text.length());
            System.out.printf("Tokens extracted: %d%n", stems.size());
            System.out.printf("Unique stems: %d%n", stems.stream().distinct().count());
            System.out.printf("Stems: %s%n", stems);

            assertFalse(stems.isEmpty(), "Should produce tokens");
            assertTrue(stems.size() >= expectedMinTokens,
                String.format("Expected at least %d tokens, got %d", expectedMinTokens, stems.size()));

            for (String prefix : expectedPrefixes)
            {
                boolean found = stems.stream().anyMatch(s -> s.startsWith(prefix));
                assertTrue(found, String.format("Expected stem starting with '%s'", prefix));
            }
        }
    }

    @Test
    @EnabledIf("isNativeAvailable")
    void testCompareWithExpectedStems()
    {
        // Known word-stem pairs for Czech
        Map<String, String> expectedStems = Map.of(
            "programování", "programov",
            "programátor", "programát",
            "program", "program"
        );

        System.out.printf("%n=== TK: Known Stem Verification ===%n");

        for (Map.Entry<String, String> entry : expectedStems.entrySet())
        {
            String word = entry.getKey();
            String expected = entry.getValue();
            String actual = stemmer.stem(word);

            System.out.printf("%-20s -> %-15s (expected: %s) %s%n",
                word, actual, expected,
                expected.equals(actual) ? "OK" : "DIFFERENT");
        }
    }

    // Helper methods

    private Map<String, List<String>> groupByStem(List<String> words)
    {
        Map<String, List<String>> stemToWords = new HashMap<>();
        for (String word : words)
        {
            String stem = stemmer.stem(word);
            if (stem != null && !stem.isEmpty())
            {
                stemToWords.computeIfAbsent(stem, k -> new ArrayList<>()).add(word);
            }
        }
        return stemToWords;
    }

    private List<String> jsonArrayToList(JsonNode arrayNode)
    {
        List<String> list = new ArrayList<>();
        for (JsonNode node : arrayNode)
        {
            list.add(node.asText());
        }
        return list;
    }

    // Condition for @EnabledIf
    static boolean isNativeAvailable()
    {
        return TKStemmer.isAvailable();
    }
}
