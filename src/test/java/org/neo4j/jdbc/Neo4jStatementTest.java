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
package org.neo4j.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.tooling.GlobalGraphOperations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.neo4j.graphdb.DynamicLabel.label;
import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * TODO
 */
public class Neo4jStatementTest extends Neo4jJdbcTest
{

    private long nodeId;

    public Neo4jStatementTest( Mode mode ) throws SQLException
    {
        super( mode );
    }

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        nodeId = createNode();
    }

    @Test
    public void testExecuteStatement() throws Exception
    {
        final ResultSet rs = conn.createStatement().executeQuery( nodeByIdQuery( nodeId ) );
        assertTrue( rs.next() );
        assertEquals( nodeId, ((Number) rs.getObject( "id" )).intValue() );
        assertEquals( nodeId, rs.getLong( "id" ) );
        assertEquals( nodeId, ((Number) rs.getObject( 1 )).intValue() );
        assertEquals( nodeId, rs.getLong( 1 ) );
        assertFalse( rs.next() );
    }

    @Test(expected = SQLException.class)
    public void testPreparedStatementMissingParameter() throws Exception
    {
        final PreparedStatement ps = conn.prepareStatement( "match (n) where id(n) = {1} return ID(n) as id" );
        final ResultSet rs = ps.executeQuery();
        rs.next();
    }

    @Test
    public void testExecutePreparedStatement() throws Exception
    {
        final PreparedStatement ps = conn.prepareStatement( "match (n) where id(n) = {1} return ID(n) as id" );
        ps.setLong( 1, nodeId );
        final ResultSet rs = ps.executeQuery();
        assertTrue( rs.next() );
        assertEquals( nodeId, ((Number) rs.getObject( "id" )).intValue() );
        assertEquals( nodeId, rs.getLong( "id" ) );
        assertEquals( nodeId, ((Number) rs.getObject( 1 )).intValue() );
        assertEquals( nodeId, rs.getLong( 1 ) );
        assertFalse( rs.next() );
    }

    @Test
    public void testCreateNodeStatement() throws Exception
    {
        final PreparedStatement ps = conn.prepareStatement( "create (n:User {name:{1}})" );
        ps.setString( 1, "test" );
        // TODO int count = ps.executeUpdate();
        int count = 0;
        ps.executeUpdate();
        begin();
        for (Node node : GlobalGraphOperations.at(gdb).getAllNodesWithLabel(label("User"))) {
            assertEquals("test", node.getProperty("name"));
            count++;
        }
        done();
        assertEquals( 1, count );
    }

    @Test
    public void testCreateNodeStatementWithMapParam() throws Exception
    {
        final PreparedStatement ps = conn.prepareStatement( "create (n:User {1})" );
        ps.setObject( 1, map( "name", "test" ) );
        // TODO int count = ps.executeUpdate();
        int count = 0;
        ps.executeUpdate();
        begin();
        for (Node node : GlobalGraphOperations.at(gdb).getAllNodesWithLabel(label("User"))) {
            assertEquals("test", node.getProperty("name"));
            count++;
        }
        done();
        assertEquals( 1, count );
    }

    @Test(expected = SQLException.class)
    public void testCreateOnReadonlyConnection() throws Exception
    {
        conn.setReadOnly( true );
        conn.createStatement().executeUpdate( "create (n {name:{1}})" );
    }

    @Test(expected = SQLDataException.class)
    public void testColumnZero() throws Exception
    {
        final ResultSet rs = conn.createStatement().executeQuery( nodeByIdQuery( nodeId ) );
        assertTrue( rs.next() );
        assertEquals( nodeId, rs.getObject( 0 ) );
        assertFalse( rs.next() );
    }

    @Test(expected = SQLDataException.class)
    public void testColumnLargerThan() throws Exception
    {
        final ResultSet rs = conn.createStatement().executeQuery( nodeByIdQuery( nodeId ) );
        rs.next();
        rs.getObject( 2 );
    }

    @Test(expected = SQLException.class)
    public void testInvalidColumnName() throws Exception
    {
        final ResultSet rs = conn.createStatement().executeQuery( nodeByIdQuery( nodeId ) );
        rs.next();
        rs.getObject( "foo" );
    }

    @Test
    public void testGetFetchDirectionIsUnknown() throws Exception {
        assertEquals(ResultSet.FETCH_UNKNOWN, conn.createStatement().getFetchDirection());
    }

    @Test
    public void testGetResultSetConcurrencyIsReadOnly() throws Exception {
        assertEquals(ResultSet.CONCUR_READ_ONLY, conn.createStatement().getResultSetConcurrency());
    }
}
