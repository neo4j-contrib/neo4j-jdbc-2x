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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.server.CommunityNeoServer;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.kernel.Version;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

/**
 * @author mh
 * @since 12.06.12
 */
@RunWith(Parameterized.class)
@Ignore
public abstract class Neo4jJdbcTest
{

    private final Driver driver;

    protected static String nodeByIdQuery( long nodeId )
    {
        return "match (n) where id(n) = " + nodeId +
                " return ID(n) as id";
    }

    protected Neo4jConnection conn;
    protected static GraphDatabaseService gdb;
    private static CommunityNeoServer webServer;
    protected final Mode mode;
    private Transaction tx;

    protected long createNode() throws SQLException
    {
        ResultSet rs = conn.createStatement().executeQuery( "merge (n:Root {name:'root'}) return id(n) as id" );
        rs.next();
        return rs.getLong( "id" );
    }

    public enum Mode
    {
        embedded, server, server_tx, server_auth
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.<Object[]>asList(new Object[]{Mode.embedded},new Object[]{Mode.server},
                                       new Object[]{Mode.server_auth},new Object[]{Mode.server_tx});
//        return Arrays.<Object[]>asList( new Object[]{Mode.server_tx} );
//        return Arrays.<Object[]>asList(new Object[]{Mode.embedded});
    }

    @BeforeClass
    public static void before()
    {
        gdb = new TestGraphDatabaseFactory().newImpermanentDatabase();
    }

    public Neo4jJdbcTest( Mode mode ) throws SQLException
    {
        this.mode = mode;
        cleanDatabase();
        driver = new Driver();
        conn = connect( mode );
    }

    protected Neo4jConnection connect( Mode mode ) throws SQLException
    {
        final Properties props = new Properties();
        switch ( mode )
        {
            case embedded:
                props.put( "db", gdb );
                return driver.connect( "jdbc:neo4j:instance:db", props );
            case server:
                if ( webServer == null )
                {
                    webServer = TestServer.startWebServer( TestServer.PORT, false );
                    updateGdbFromServer();
                }
                props.setProperty( Driver.LEGACY, "true" );
                return driver.connect( "jdbc:neo4j://localhost:" + TestServer.PORT, props );
            case server_tx:
                if ( webServer == null )
                {
                    webServer = TestServer.startWebServer( TestServer.PORT, false );
                    updateGdbFromServer();
                }
                return driver.connect( "jdbc:neo4j://localhost:" + TestServer.PORT, props );
            case server_auth:
                if ( webServer == null )
                {
                    webServer = TestServer.startWebServer( TestServer.PORT, true );
                    updateGdbFromServer();
                }
                props.put( Driver.USER, TestAuthenticationFilter.USER );
                props.put( Driver.PASSWORD, TestAuthenticationFilter.PASSWORD );
                props.setProperty( Driver.LEGACY, "true" );
                return driver.connect( "jdbc:neo4j://localhost:" + TestServer.PORT, props );
        }
        throw new IllegalStateException( "Unknown mode "+ mode );
    }

    private void updateGdbFromServer()
    {
        if (gdb !=null ) { gdb.shutdown(); }
        gdb = webServer.getDatabase().getGraph();
    }


    @AfterClass
    public static void after()
    {
        try
        {
            if ( webServer != null )
            {
                webServer.stop();
                webServer = null;
            }
            gdb.shutdown();
        }
        catch ( Throwable e )
        {
            e.printStackTrace();
        }
    }

    @Before
    public void setUp() throws SQLException, Exception
    {
        cleanDatabase();
    }

    protected String jdbcUrl()
    {
        return "jdbc:neo4j://localhost:" + TestServer.PORT + "/";
    }

    @After
    public void tearDown() throws Exception
    {
        try
        {
            if ( conn != null )
            {
                conn.close();
            }
        }
        catch ( Throwable e )
        {
            e.printStackTrace();
        }
    }

    protected void createTableMetaData( GraphDatabaseService gdb, String typeName, String propName, String propType )
    {
        final Transaction tx = gdb.beginTx();
        try
        {
            final Node root = gdb.createNode( DynamicLabel.label( "MetaDataRoot" ) );
            final Node type = gdb.createNode();
            type.setProperty( "type", typeName );
            root.createRelationshipTo( type, DynamicRelationshipType.withName( "TYPE" ) );
            final Node property = gdb.createNode();
            property.setProperty( "name", propName );
            property.setProperty( "type", propType );
            type.createRelationshipTo( property, DynamicRelationshipType.withName( "HAS_PROPERTY" ) );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    protected void dumpColumns( ResultSet rs ) throws SQLException
    {
        final ResultSetMetaData meta = rs.getMetaData();
        final int cols = meta.getColumnCount();
        for ( int col = 1; col < cols; col++ )
        {
            System.out.println( meta.getColumnName( col ) );
        }
    }

    protected Version getVersion()
    {
        return org.neo4j.kernel.Version.getKernel();
    }

    protected Transaction begin()
    {
        return tx = gdb.beginTx();
    }

    protected void done()
    {
        if ( tx != null )
        {
            tx.success();
            tx.close();
        }
    }

    private void cleanDatabase() {
        try (Transaction tx = gdb.beginTx()) {
            gdb.execute("MATCH n DETACH DELETE n");
            tx.success();
        }
    }

}
