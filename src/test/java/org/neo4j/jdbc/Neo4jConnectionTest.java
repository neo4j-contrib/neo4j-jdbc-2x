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

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.tooling.GlobalGraphOperations;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.neo4j.graphdb.DynamicLabel.label;

public class Neo4jConnectionTest extends Neo4jJdbcTest
{

    private String columName = "propName";
    private String tableName = "test";
    private String columnPrefix = "_";

    public Neo4jConnectionTest( Mode mode ) throws SQLException
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
        createTableMetaData( gdb, tableName, columName, "String");
    }

    @Test
    public void testAccessData() throws Exception
    {
        try ( Transaction tx = gdb.beginTx() )
        {
            ResourceIterable<Node> nodes = GlobalGraphOperations.at(gdb).getAllNodesWithLabel(label("MetaDataRoot"));
            final Node root = IteratorUtil.single(nodes);
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
        final Iterable<String> res = conn.returnProperties( tableName, columnPrefix );
        boolean found = false;
        for ( String expression : res )
        {
            assertEquals( columnPrefix + columName, expression );
            found = true;
        }
        assertTrue( found );
    }
}
