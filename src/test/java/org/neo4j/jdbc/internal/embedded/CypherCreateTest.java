package org.neo4j.jdbc.internal.embedded;

import java.util.Collections;

import org.junit.Test;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.test.ImpermanentGraphDatabase;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 15.06.12
 */
public class CypherCreateTest
{
    @Test
    public void testCreateNodeWithParam() throws Exception
    {
        final ImpermanentGraphDatabase gdb = new ImpermanentGraphDatabase();
        final ExecutionEngine engine = new ExecutionEngine( gdb );
        ExecutionResult result = engine.execute( "create (n {name:{1}}) return id(n) as id", Collections.<String,
                Object>singletonMap( "1", "test" ) );
        Long id = IteratorUtil.single( result.<Long>columnAs( "id" ) );
        try ( Transaction tx = gdb.beginTx() )
        {
            final Node node = gdb.getNodeById( id );
            assertEquals( "test", node.getProperty( "name" ) );
            tx.success();
        }
    }
}
