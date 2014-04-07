package org.neo4j.jdbc.internal.embedded;

import org.junit.Test;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.jdbc.internal.ExecutionResult;
import org.neo4j.test.TestGraphDatabaseFactory;

import static java.util.Arrays.asList;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 15.06.12
 */
public class EmbeddedQueryExecutorTest
{
    @Test
    public void testDoExecuteQuery() throws Exception
    {
        GraphDatabaseService db = new TestGraphDatabaseFactory().newImpermanentDatabase();
        long nodeId = createNode( db );
        final EmbeddedQueryExecutor executor = new EmbeddedQueryExecutor( db );
        final ExecutionResult result = executor.executeQuery( "start n=node(" + nodeId + ") return ID(n) as id",
                null, true );
        assertEquals( asList( "id" ), result.columns() );
        final Object[] row = result.iterator().next();
        assertEquals( 1, row.length );
        assertEquals( nodeId, row[0] );
    }

    private long createNode( GraphDatabaseService db )
    {
        try ( Transaction tx = db.beginTx() )
        {
            Node node = db.createNode();
            tx.success();
            return node.getId();
        }
    }
}
