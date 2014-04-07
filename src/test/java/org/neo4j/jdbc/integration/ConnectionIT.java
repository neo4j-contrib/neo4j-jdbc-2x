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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.neo4j.cypherdsl.Property;
import org.neo4j.cypherdsl.expression.Expression;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.tooling.GlobalGraphOperations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConnectionIT extends Neo4jJdbcIntegrationTest
{

    private String columName = "propName";
    private String tableName = "test";
    private String columnPrefix = "_";
    private final String columnType = "String";

    public ConnectionIT( Mode mode ) throws SQLException
    {
        super( mode );
    }

    @Test
    public void testGetMetaData() throws SQLException
    {
        DatabaseMetaData metaData = conn.getMetaData();
        Assert.assertThat( metaData, CoreMatchers.<DatabaseMetaData>notNullValue() );
        final String productVersion = metaData.getDatabaseProductVersion();
        final String dbVersion = getVersion().getVersion();
        Assert.assertTrue( productVersion + " != " + dbVersion, productVersion.startsWith( dbVersion.substring( 0,
                2 ) ) );
    }

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        createTableMetaData( gdb, tableName, columName, columnType );
    }

    @Test
    public void testAccessData() throws Exception
    {
        try ( Transaction tx = gdb.beginTx() )
        {
            final Node root = IteratorUtil.single( GlobalGraphOperations.at( gdb ).getAllNodesWithLabel( DynamicLabel
                    .label( "MetaDataRoot" ) ) );
            final Relationship typeRel = root.getSingleRelationship( DynamicRelationshipType.withName( "TYPE" ),
                    Direction.OUTGOING );
            final Node typeNode = typeRel.getEndNode();
            assertEquals( "test", typeNode.getProperty( "type" ) );
            tx.success();
        }
    }

    @Test
    public void testGetTableMetaDataTables() throws Exception
    {
        final ResultSet rs = conn.getMetaData().getTables( null, null, tableName, null );
        assertTrue( rs.next() );
        assertEquals( tableName, rs.getString( "TABLE_NAME" ) );
        assertFalse( rs.next() );
    }

    @Test
    public void testGetTableMetaDataColumns() throws Exception
    {
        final ResultSet rs = conn.getMetaData().getColumns( null, null, tableName, null );
        assertTrue( rs.next() );
        dumpColumns( rs );
        assertEquals( tableName, rs.getString( "TABLE_NAME" ) );
        assertEquals( columName, rs.getString( "COLUMN_NAME" ) );
        assertEquals( Types.VARCHAR, rs.getInt( "DATA_TYPE" ) );
        assertFalse( rs.next() );
    }

    @Test
    public void testTableColumns() throws Exception
    {
        final String res = conn.tableColumns( tableName, columnPrefix );
        assertEquals( columnPrefix + columName, res );
    }

    @Test
    public void testProperties() throws Exception
    {
        final Iterable<Expression> res = conn.returnProperties( tableName, columnPrefix );
        boolean found = false;
        for ( Expression expression : res )
        {
            assertTrue( expression instanceof Property );
            final Property property = (Property) expression;
            assertEquals( columName, property.toString() );
            found = true;
        }
        assertTrue( found );
    }
}
