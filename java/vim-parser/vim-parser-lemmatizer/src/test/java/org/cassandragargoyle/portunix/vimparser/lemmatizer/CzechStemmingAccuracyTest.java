/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.vimparser.lemmatizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cassandragargoyle.portunix.vimparser.lemmatizer.backend.lucene.LuceneBackend;
import org.cassandragargoyle.portunix.vimparser.model.StemmedResult;
import org.cassandragargoyle.portunix.vimparser.model.StemmedToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Accuracy test for Czech stemming
 * Test data is loaded from czech-stemming-test-data.json
 *
 * @author Zdenek
 * @since 2026-01-31
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

	private LuceneBackend backend;

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
		backend = new LuceneBackend();
	}

	@AfterEach
	void tearDown()
	{
		backend.close();
	}

	@Test
	void testNounDeclensionConsistency()
	{
		JsonNode nouns = testData.get("nouns");
		int totalGroups = nouns.size();
		int consistentGroups = 0;

		System.out.println("\n=== Czech Noun Declension Stemming Test ===\n");

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
	void testAdjectiveConsistency()
	{
		JsonNode adjectives = testData.get("adjectives");
		int totalGroups = adjectives.size();
		int consistentGroups = 0;

		System.out.println("\n=== Czech Adjective Declension Stemming Test ===\n");

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
	void testOverallNounStemmingAccuracy()
	{
		JsonNode nouns = testData.get("nouns");
		int totalWords = 0;
		int wordsWithMajorityStem = 0;

		System.out.println("\n=== Overall Noun Stemming Accuracy Test ===\n");

		for (JsonNode noun : nouns)
		{
			List<String> forms = jsonArrayToList(noun.get("forms"));
			Map<String, Integer> stemCounts = new HashMap<>();

			for (String word : forms)
			{
				StemmedResult result = backend.stem(word, language);
				if (!result.getTokens().isEmpty())
				{
					String stem = result.getTokens().get(0).getStem();
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
	void testProcessingSpeed()
	{
		List<String> words = jsonArrayToList(testData.get("speedTestWords"));
		int repeat = testData.get("speedTestRepeat").asInt();

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < repeat; i++)
		{
			sb.append(words.get(i % words.size())).append(" ");
		}
		String largeText = sb.toString();

		// Warm up
		backend.stem(largeText, language);

		// Measure
		long startTime = System.nanoTime();
		StemmedResult result = backend.stem(largeText, language);
		long endTime = System.nanoTime();

		double elapsedSeconds = (endTime - startTime) / 1_000_000_000.0;
		double tokensPerSecond = result.getTokens().size() / elapsedSeconds;

		System.out.printf("%n=== Processing Speed Test ===%n");
		System.out.printf("Tokens processed: %d%n", result.getTokens().size());
		System.out.printf("Time: %.3f seconds%n", elapsedSeconds);
		System.out.printf("Speed: %.0f tokens/second%n", tokensPerSecond);

		assertTrue(tokensPerSecond > minTokensPerSecond,
			String.format("Processing speed %.0f tokens/sec is below required %d tokens/sec",
				tokensPerSecond, minTokensPerSecond));
	}

	@Test
	void testSampleTextProcessing()
	{
		JsonNode samples = testData.get("sampleTexts");

		for (JsonNode sample : samples)
		{
			String name = sample.get("name").asText();
			String text = sample.get("text").asText();
			int expectedMinTokens = sample.get("expectedMinTokens").asInt();
			List<String> expectedPrefixes = jsonArrayToList(sample.get("expectedStemPrefixes"));

			StemmedResult result = backend.stem(text, language);

			System.out.printf("%n=== Sample Text: %s ===%n", name);
			System.out.printf("Input length: %d chars%n", text.length());
			System.out.printf("Tokens extracted: %d%n", result.getTokens().size());
			System.out.printf("Unique stems: %d%n", result.getStatistics().getUniqueStems());

			List<String> stems = result.getTokens().stream()
				.map(StemmedToken::getStem)
				.toList();
			System.out.printf("Stems: %s%n", stems);

			assertFalse(result.getTokens().isEmpty(), "Should produce tokens");
			assertTrue(result.getTokens().size() >= expectedMinTokens,
				String.format("Expected at least %d tokens, got %d", expectedMinTokens, result.getTokens().size()));

			for (String prefix : expectedPrefixes)
			{
				boolean found = stems.stream().anyMatch(s -> s.startsWith(prefix));
				assertTrue(found, String.format("Expected stem starting with '%s'", prefix));
			}
		}
	}

	// Helper methods

	private Map<String, List<String>> groupByStem(List<String> words)
	{
		Map<String, List<String>> stemToWords = new HashMap<>();
		for (String word : words)
		{
			StemmedResult result = backend.stem(word, language);
			if (!result.getTokens().isEmpty())
			{
				String stem = result.getTokens().get(0).getStem();
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
}
