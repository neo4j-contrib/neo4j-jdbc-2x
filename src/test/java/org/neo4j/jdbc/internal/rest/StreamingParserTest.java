package org.neo4j.jdbc.internal.rest;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.jdbc.internal.ExecutionResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author mh
 * @since 19.04.13
 */
public class StreamingParserTest
{

    private StreamingParser streamingParser;

    @Before
    public void setUp() throws Exception
    {
        streamingParser = new StreamingParser( new ObjectMapper() );
    }

    @Test
    public void testSkipStartObject() throws Exception
    {
        final String json = "{\"foo\":32}";
        final StreamingParser.ParserState parser = streamingParser.obtainParserState( new StringReader( json ) );
        streamingParser.skipTo( parser, "", JsonToken.START_OBJECT );
        Assert.assertEquals( JsonToken.FIELD_NAME, parser.nextToken() );
        Assert.assertEquals( "foo", parser.getCurrentName() );
    }

    @Test
    public void testSkipStartObjectField() throws Exception
    {
        final String json = "{\"foo\":42}";
        final StreamingParser.ParserState parser = streamingParser.obtainParserState( new StringReader( json ) );
        streamingParser.skipTo( parser, "", JsonToken.START_OBJECT, "foo" );
        Assert.assertEquals( JsonToken.VALUE_NUMBER_INT, streamingParser.nextToken( parser ) );
        Assert.assertEquals( 42, parser.parser.getValueAsInt() );
    }

    @Test
    public void testReadArray() throws Exception
    {
        final String json = "{\"foo\":[1,2,3]}";
        final StreamingParser.ParserState parser = streamingParser.obtainParserState( new StringReader( json ) );
        streamingParser.skipTo( parser, "", JsonToken.START_OBJECT, "foo" );
        final Object[] objects = streamingParser.readObjectArray( parser );
        Assert.assertTrue( Arrays.toString( objects ), Arrays.equals( new Integer[]{1, 2, 3}, objects ) );
    }

    @Test
    public void testReadStringList() throws Exception
    {
        final String json = "{\"columns\":[\"a\",\"b\",\"c\"]}";
        final StreamingParser.ParserState parser = streamingParser.obtainParserState( new StringReader( json ) );
        streamingParser.skipTo( parser, "", JsonToken.START_OBJECT, "columns" );
        List<String> columns = streamingParser.readList( parser );
        assertEquals( asList( "a", "b", "c" ), columns );
    }

    @Test
    public void testReadResultWithOneRow() throws Exception
    {
        final String json = "{\"columns\":[\"a\",\"b\",\"c\"],\"data\": [{\"row\":[1,2,3]}]}";
        final StreamingParser.ParserState parser = streamingParser.obtainParserState( new StringReader( json ) );
        final ExecutionResult result = streamingParser.nextResult( parser );
        assertEquals( asList( "a", "b", "c" ), result.columns() );
        final Iterator<Object[]> rows = result.iterator();
        assertArrayEquals( new Integer[]{1, 2, 3}, rows.next() );
        assertFalse( rows.hasNext() );
    }

    @Test(expected = IllegalStateException.class)
    public void testReadResultWithIncorrectRowLength() throws Exception
    {
        final String json = "{\"columns\":[\"a\",\"b\"],\"data\": [{\"row\":[1]}]}";
        final StreamingParser.ParserState parser = streamingParser.obtainParserState( new StringReader( json ) );
        final ExecutionResult result = streamingParser.nextResult( parser );
        IteratorUtil.count( result );
        assertEquals( asList( "a", "b", "c" ), result.columns() );
    }

    @Test
    public void testReadResultWithComplexContent() throws Exception
    {
        final String json = "{\"columns\":[\"a\",\"b\",\"c\"],\"data\": [{\"row\":[[],{\"foo\":\"bar\"},[{},{}]]}]}";
        final StreamingParser.ParserState parser = streamingParser.obtainParserState( new StringReader( json ) );
        final ExecutionResult result = streamingParser.nextResult( parser );
        assertEquals( asList( "a", "b", "c" ), result.columns() );
        final Iterator<Object[]> rows = result.iterator();
        final Object[] row = rows.next();
        assertEquals( Collections.emptyList(), row[0] );
        assertEquals( Collections.singletonMap( "foo", "bar" ), row[1] );
        assertEquals( Arrays.asList( emptyMap(), emptyMap() ), row[2] );
        assertFalse( rows.hasNext() );
    }

    @Test
    public void testReadResultWithTwoRows() throws Exception
    {
        final String json = "{\"columns\":[\"a\",\"b\",\"c\"],\"data\": [{\"row\":[1,2,3]},{\"row\":[4,5,6]}]}";
        final StreamingParser.ParserState parser = streamingParser.obtainParserState( new StringReader( json ) );
        final ExecutionResult result = streamingParser.nextResult( parser );
        assertEquals( asList( "a", "b", "c" ), result.columns() );
        final Iterator<Object[]> rows = result.iterator();
        assertArrayEquals( new Integer[]{1, 2, 3}, rows.next() );
        assertArrayEquals( new Integer[]{4, 5, 6}, rows.next() );
        assertFalse( rows.hasNext() );
    }

    static final Statement STATEMENT = new Statement( "start n=node(0) return n", Collections.<String,
            Object>emptyMap() );

    @Test
    public void testReadMultipleResults() throws Exception
    {
        final String resultJson1 = "{\"columns\":[\"a\",\"b\",\"c\"],\"data\": [{\"row\":[1,2,3]}]}";
        final String resultJson2 = "{\"columns\":[\"d\",\"e\",\"f\"],\"data\": [{\"row\":[4,5,6]}]}";
        final String json = "{\"results\":[" + resultJson1 + "," + resultJson2 + "]}";
        final JsonParser parser = streamingParser.obtainParser( new StringReader( json ) );
        final Iterator<ExecutionResult> results = streamingParser.toResults( parser, null, STATEMENT, STATEMENT );
        ExecutionResult result = results.next();
        assertEquals( asList( "a", "b", "c" ), result.columns() );
        final Iterator<Object[]> rows = result.iterator();
        assertArrayEquals( new Integer[]{1, 2, 3}, rows.next() );
        assertFalse( rows.hasNext() );
        result = results.next();
        assertEquals( asList( "d", "e", "f" ), result.columns() );
        assertFalse( results.hasNext() );
    }
}
