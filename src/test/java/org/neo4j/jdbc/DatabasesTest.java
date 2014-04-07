package org.neo4j.jdbc;

import java.io.File;
import java.util.Properties;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.jdbc.internal.embedded.EmbeddedDatabases;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.EmbeddedReadOnlyGraphDatabase;
import org.neo4j.kernel.impl.util.FileUtils;
import org.neo4j.test.ImpermanentGraphDatabase;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author mh
 * @since 15.06.12
 */
public class DatabasesTest
{

    private EmbeddedDatabases databases;

    @Before
    public void setUp() throws Exception
    {
        databases = new EmbeddedDatabases();
    }

    @Test
    public void testLocateMem() throws Exception
    {
        assertTrue( databases.createDatabase( ":mem", null ) instanceof ImpermanentGraphDatabase );
    }

    @Test
    public void testLocateNamedMem() throws Exception
    {
        final GraphDatabaseService db = databases.createDatabase( ":mem:a", null );
        assertTrue( db instanceof ImpermanentGraphDatabase );
        final GraphDatabaseService db2 = databases.createDatabase( ":mem:a", null );
        assertSame( db2, db );
    }

    @Test
    public void testLocateNamedInstance() throws Exception
    {
        final GraphDatabaseService db = new ImpermanentGraphDatabase();
        final Properties props = new Properties();
        props.put( "a", db );
        final GraphDatabaseService db2 = databases.createDatabase( ":instance:a", props );
        assertSame( db2, db );
    }

    @Test
    public void testLocateFileDb() throws Exception
    {
        FileUtils.deleteRecursively( new File( "target/test-db" ) );
        final GraphDatabaseService db = databases.createDatabase( ":file:target/test-db", null );
        assertTrue( db instanceof EmbeddedGraphDatabase );
        final GraphDatabaseService db2 = databases.createDatabase( ":file:target/test-db", null );
        assertSame( db2, db );
    }

    @Test
    @Ignore("Not possible anymore to have a rw and ro-db accesssing the same store-files")
    public void testLocateFileDbReadonly() throws Exception
    {
        FileUtils.deleteRecursively( new File( "target/test-db-ro" ) );
        GraphDatabaseService db0 = new GraphDatabaseFactory().newEmbeddedDatabase( "target/test-db-ro" );
        try
        {
            final Properties props = new Properties();
            props.setProperty( "readonly", "true" );
            final GraphDatabaseService db = databases.createDatabase( ":file:target/test-db-ro", props );
            try
            {
                assertTrue( db instanceof EmbeddedReadOnlyGraphDatabase );
            }
            finally
            {
                db.shutdown();
            }
        }
        finally
        {
            db0.shutdown();
        }
    }
}
