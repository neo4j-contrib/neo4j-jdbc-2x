package org.neo4j.jdbc.rest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JsonUtilsTest {

	@Test
	public void testEscapeHandlesSingleQuoteValues() {
		String input = "MATCH (director { name:'Oliver Stone' })--(movie) RETURN movie.title";
		assertEquals(input, JsonUtils.escapeQuery(input));
	}
	
	@Test
	public void testEscapeHandlesNonEscapedDoubleQuotesInValues() {
		String input    = "CREATE (n:Node { json:'{\"key\":\"value\"}' })";
		String expected = "CREATE (n:Node { json:'{\\\"key\\\":\\\"value\\\"}' })"; 
		assertEquals(expected, JsonUtils.escapeQuery(input));
	}
	
	@Test
	public void testEscapeIgnoresEscapedDoubleQuotesInValues() {
		String input = "CREATE (n:Node { json:'{\\\"key\\\":\\\"value\\\"}' })";
		assertEquals(input, JsonUtils.escapeQuery(input));
	}

	@Test
	public void testEscapeHandlesNestedEscapedDoubleQuotesInValues() {
		String input    = "CREATE (n:Node { json:\"{\\\"key\\\":\\\"value\\\"}\" })";
		String expected = "CREATE (n:Node { json:'{\\\"key\\\":\\\"value\\\"}' })";
		assertEquals(expected, JsonUtils.escapeQuery(input));
	}
	
	@Test
	public void testEscapeReplacesDoubleQuoteCypherDelimiters() {
		String input = "MATCH (director { name:\"Oliver Stone\" })--(movie) RETURN movie.title";
		String expected = "MATCH (director { name:'Oliver Stone' })--(movie) RETURN movie.title";
		assertEquals(expected, JsonUtils.escapeQuery(input));
	}

	@Test
	public void testEscapeWithComplexMixedTypes() {
		String input    = "CREATE (n:Node { json:'{\\\"key\\\":\\\"it\\'s complicated\\\"}', ue:'{\"key\":\"value\"}', id:\"abcd\" })";
		String expected = "CREATE (n:Node { json:'{\\\"key\\\":\\\"it\\'s complicated\\\"}', ue:'{\\\"key\\\":\\\"value\\\"}', id:'abcd' })";
		assertEquals(expected, JsonUtils.escapeQuery(input));
	}
	
}
