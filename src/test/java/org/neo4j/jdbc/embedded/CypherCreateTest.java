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
package org.neo4j.jdbc.embedded;

import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.util.Collections;

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
        final GraphDatabaseService gdb = new TestGraphDatabaseFactory().newImpermanentDatabase();
        ExecutionEngine executionEngine = new ExecutionEngine(gdb);
        ExecutionResult result = executionEngine.execute( "create (n {name:{1}}) return id(n) as id", Collections.<String,
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
