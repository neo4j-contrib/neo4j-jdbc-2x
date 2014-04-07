package org.neo4j.jdbc.integration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import static org.neo4j.helpers.collection.MapUtil.map;

public class TransactionIT extends Neo4jJdbcIntegrationTest
{
    public TransactionIT( Mode mode ) throws SQLException
    {
        super( mode );
    }

    @Test
    public void testCreateNodeWithAutoCommit() throws Exception
    {
        final ResultSet rs = conn.executeQuery( "create (n {name:'Andres'}) return id(n)", null );
        rs.next();
        final long nodeId = rs.getLong( 1 );
        begin();
        assertEquals( nodeId, gdb.getNodeById( nodeId ).getId() );
        done();
    }

    @Test
    public void testAutoCommit() throws Exception
    {
        conn.setAutoCommit( true );
        final ResultSet rs = conn.executeQuery( "create (n {name:'Andres'}) return id(n)", null );
        rs.next();
        final long nodeId = rs.getLong( 1 );
        begin();
        assertEquals( nodeId, gdb.getNodeById( nodeId ).getId() );
        done();
    }

    @Test
    public void testManualCommit() throws Exception
    {
        try
        {
            conn.setAutoCommit( false );
            final ResultSet rs = conn.executeQuery( "create (n {name:'Andres'}) return id(n)", null );
            rs.next();
            final long nodeId = rs.getLong( 1 );
            assertNodeExists( nodeId, true );
            assertNodeVisibleInOtherTransaction( nodeId, false );
            conn.commit();
            assertNodeVisibleInOtherTransaction( nodeId, true );
            assertNodeExists( nodeId, true );
        }
        catch ( SQLException sqle )
        {
            if ( sqle.getMessage().contains( "not supported" ) )
            {
                return;
            }
            throw sqle;
        }
    }

    @Test
    public void testSetAutoCommitOnCommitsAccordingToSpec() throws Exception
    {
        try
        {
            conn.setAutoCommit( false );
            final ResultSet rs = conn.executeQuery( "create (n {name:'Andres'}) return id(n)", null );
            rs.next();
            final long nodeId = rs.getLong( 1 );
            assertNodeExists( nodeId, true );
            assertNodeVisibleInOtherTransaction( nodeId, false );
            conn.setAutoCommit( true );
            assertNodeVisibleInOtherTransaction( nodeId, true );
            assertNodeExists( nodeId, true );
        }
        catch ( SQLException sqle )
        {
            if ( sqle.getMessage().contains( "not supported" ) )
            {
                return;
            }
            throw sqle;
        }
    }

    @Test
    public void testManualRollback() throws Exception
    {
        try
        {
            conn.setAutoCommit( false );
            final ResultSet rs = conn.executeQuery( "create (n {name:'Andres'}) return id(n)", null );
            rs.next();
            final long nodeId = rs.getLong( 1 );
            assertNodeExists( nodeId, true );
            assertNodeVisibleInOtherTransaction( nodeId, false );
            conn.rollback();
            assertNodeVisibleInOtherTransaction( nodeId, false );
            assertNodeExists( nodeId, false );
        }
        catch ( SQLException sqle )
        {
            if ( sqle.getMessage().contains( "not supported" ) )
            {
                return;
            }
            throw sqle;
        }
    }

    private void assertNodeExists( long nodeId, boolean exists ) throws SQLException
    {
        // work around the automatic node-by-id lookup in cypher
        final ResultSet found = conn.executeQuery( "start n=node(*) where has(n.name) AND id(n) = {id} return id(n)",
                map( "id", nodeId ) );
        assertEquals( "node exists " + nodeId, exists, found.next() );
        if ( exists )
        {
            assertEquals( exists, nodeId == found.getLong( 1 ) );
        }
    }

    private void assertNodeVisibleInOtherTransaction( final long nodeId, final boolean exists ) throws
            InterruptedException
    {
        final AtomicBoolean valid = new AtomicBoolean();
        final Thread t = new Thread()
        {
            public void run()
            {
                try
                {
                    assertNodeExists( nodeId, exists );
                    valid.set( true );
                }
                catch ( SQLException e )
                {
                    // ignore
                }
            }
        };
        t.start();
        t.join();
        assertEquals( "Node found in a separate thread", true, valid.get() );
    }

    @After
    public void unsetManualCommitMode() throws Exception
    {
        conn.setAutoCommit( true );
    }
}
