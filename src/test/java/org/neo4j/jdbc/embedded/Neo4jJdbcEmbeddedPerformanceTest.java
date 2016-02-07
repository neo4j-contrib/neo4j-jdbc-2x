/**
 * Licensed to Neo Technology under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Neo Technology licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.neo4j.jdbc.embedded;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.jdbc.Neo4jJdbcPerformanceTestRunner;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * @author mh
 * @since 14.06.12
 */
public class Neo4jJdbcEmbeddedPerformanceTest
{

    private GraphDatabaseService gdb;
    private Neo4jJdbcPerformanceTestRunner runner;

    @Before
    public void setUp() throws Exception
    {
        gdb = new TestGraphDatabaseFactory().newImpermanentDatabase();
        runner = new Neo4jJdbcPerformanceTestRunner( gdb );
    }

    @Test
    public void testExecuteStatementEmbedded() throws Exception
    {
        final Properties props = new Properties();
        props.put( "db", gdb );
        final Connection con = DriverManager.getConnection( "jdbc:neo4j:instance:db", props );
        runner.executeMultiple( con );
    }
}
