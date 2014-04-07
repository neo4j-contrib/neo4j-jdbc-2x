package org.neo4j.jdbc.internal.rest;

import java.sql.SQLException;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.jdbc.Neo4jJdbcPerformanceTestRunner;
import org.neo4j.jdbc.integration.Neo4jJdbcIntegrationTest;

@Ignore("Perf-Test")
public class Neo4jJdbcRestPerformanceTest extends Neo4jJdbcIntegrationTest
{

    private Neo4jJdbcPerformanceTestRunner runner;

    public Neo4jJdbcRestPerformanceTest( Neo4jJdbcIntegrationTest.Mode mode ) throws SQLException
    {
        super( mode );
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        runner = new Neo4jJdbcPerformanceTestRunner( gdb );
    }

    @Test
    public void testExecuteStatement() throws Exception
    {
        runner.executeMultiple( conn );
    }
}
