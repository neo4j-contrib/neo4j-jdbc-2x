/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.neo4j.jdbc.integration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Node;
import org.neo4j.tooling.GlobalGraphOperations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.neo4j.helpers.collection.MapUtil.map;

public class StatementIT extends Neo4jJdbcIntegrationTest
{
    private long nodeId;

    public StatementIT( Mode mode ) throws SQLException
    {
        super( mode );
    }

    @Before
    @Override
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
        final PreparedStatement ps = conn.prepareStatement( "start n=node({1}) return ID(n) as id" );
        final ResultSet rs = ps.executeQuery();
        rs.next();
    }

    @Test
    public void testExecutePreparedStatement() throws Exception
    {
        final PreparedStatement ps = conn.prepareStatement( "start n=node({1}) return ID(n) as id" );
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
        for ( Node node : GlobalGraphOperations.at( gdb ).getAllNodesWithLabel( DynamicLabel.label( "User" ) ) )
        {
            assertEquals( "test", node.getProperty( "name" ) );
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
        for ( Node node : GlobalGraphOperations.at( gdb ).getAllNodesWithLabel( DynamicLabel.label( "User" ) ) )
        {
            assertEquals( "test", node.getProperty( "name" ) );
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
}
