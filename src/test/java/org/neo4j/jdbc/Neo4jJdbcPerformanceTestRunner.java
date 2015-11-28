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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

/**
 * @author mh
 * @since 14.06.12
 */
public class Neo4jJdbcPerformanceTestRunner
{

    private static final int RUNS = 10;
    private static final int COUNT = 5000;

    public Neo4jJdbcPerformanceTestRunner( GraphDatabaseService gdb )
    {
        createData( gdb, COUNT );
    }

    private void createData( GraphDatabaseService gdb, int count )
    {
        final DynamicRelationshipType type = DynamicRelationshipType.withName( "RELATED_TO" );
        final Transaction tx = gdb.beginTx();
        try
        {
            final Node node = gdb.createNode();
            for ( int i = 0; i < count; i++ )
            {
                final Node n = gdb.createNode();
                node.createRelationshipTo( n, type );
            }
            tx.success();
        }
        finally
        {
            tx.close();
        }

    }

    private long execute( final Connection con ) throws SQLException
    {
        long time = System.currentTimeMillis();
        final ResultSet rs = con.createStatement().executeQuery( "match (n) match p=n-[r]->m return n," +
                "ID(n) as id, r,m,p" );
        int count = 0;
        while ( rs.next() )
        {
            rs.getInt( "id" );
            count++;
        }
        return System.currentTimeMillis() - time;
    }

    public void executeMultiple( Connection con ) throws SQLException
    {
        long delta = 0;
        execute( con );
        for ( int i = 0; i < RUNS; i++ )
        {
            delta += execute( con );
        }
        System.out.println( "Query took " + delta / RUNS + " ms." );
    }

}
