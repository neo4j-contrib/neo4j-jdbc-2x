package org.neo4j.jdbc.internal.ext;

import java.sql.SQLException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import org.neo4j.jdbc.integration.Neo4jJdbcIntegrationTest;
import org.neo4j.jdbc.internal.Connections;

/**
 * @author mh
 * @since 12.06.12
 */
@Ignore
public class DbVisualizerConnectionTest extends Neo4jJdbcIntegrationTest
{
    public DbVisualizerConnectionTest( Mode mode ) throws SQLException
    {
        super( mode );
    }

    @BeforeClass
    public static void setDBVisualizer() throws Exception
    {
        System.setProperty( Connections.DB_VIS, "true" );
    }

    @AfterClass
    public static void removeDBVisualizer()
    {
        System.clearProperty( Connections.DB_VIS );
    }

    @Test
    public void testExecuteQuery() throws Exception
    {
        conn.createStatement().executeQuery( DbVisualizerConnection.COLUMNS_QUERY + " \"foo\"" );
    }
}
