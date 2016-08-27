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

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.Exceptions;
import org.neo4j.helpers.collection.IteratorWrapper;
import org.neo4j.jdbc.ExecutionResult;
import org.neo4j.jdbc.QueryExecutor;
import org.neo4j.jdbc.Version;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author mh
 * @since 15.06.12
 */
public class EmbeddedQueryExecutor implements QueryExecutor
{


    private final GraphDatabaseService gds;
    private final ExecutionEngine executionEngine;

    ThreadLocal<Transaction> tx = new ThreadLocal<Transaction>();

    public EmbeddedQueryExecutor( GraphDatabaseService gds )
    {
        this.gds = gds;
        this.executionEngine = new ExecutionEngine(gds);
    }

    @Override
    public ExecutionResult executeQuery( final String query, Map<String, Object> parameters,
                                         final boolean autoCommit ) throws Exception
    {
        final Map<String, Object> params = parameters == null ? Collections.<String, Object>emptyMap() : parameters;
        begin();
        final org.neo4j.cypher.javacompat.ExecutionResult result = executionEngine.execute( query, params );
        final List<String> columns = result.columns();
        final int cols = columns.size();
        final Object[] resultRow = new Object[cols];
        final ResourceIterator<Map<String, Object>> iterator = result.iterator();
        if ( !iterator.hasNext())
        {
            commitIfAutoCommit( autoCommit );
        }
        return new ExecutionResult( columns, new IteratorWrapper<Object[], Map<String, Object>>( iterator )
        {
            boolean closed = false;

            @Override
            public Object[] next()
            {
                try
                {
                    return super.next();
                }
                catch ( Exception e )
                {
                    handleException( e, query );
                    return null; // This will never happen
                }
                finally
                {
                    if ( !hasNext() && !closed )
                    {
                        close();
                    }
                }
            }

            protected Object[] underlyingObjectToObject( Map<String, Object> row )
            {
                for ( int i = 0; i < cols; i++ )
                {
                    resultRow[i] = row.get( columns.get( i ) );
                }
                return resultRow;
            }

            public void close()
            {
                iterator.close();
                closed = true;
                commitIfAutoCommit( autoCommit );
            }
        } );
    }

    private void commitIfAutoCommit( boolean autoCommit )
    {
        if ( autoCommit )
        {
            try
            {
                commit();
            }
            catch ( Exception e )
            {
                throw Exceptions.launderedException( e );
            }
        }
    }

    private void begin()
    {
        if ( tx.get() == null )
        {
            tx.set( gds.beginTx() );
        }
    }

    @Override
    public void commit() throws Exception
    {
        final Transaction transaction = tx.get();
        if ( transaction == null )
        {
            return; // throw new SQLException("Not in transaction for commit");
        }
        tx.set( null );
        transaction.success();
        transaction.close();
    }

    @Override
    public void rollback() throws Exception
    {
        final Transaction transaction = tx.get();
        if ( transaction == null )
        {
            return;
        }
        tx.set( null );
        transaction.failure();
        transaction.close();
    }

    private void handleException( Exception cause, String query )
    {
        final SQLException sqlException = new SQLException( "Error executing query: " + query, cause );
        AnyThrow.unchecked( sqlException );
    }

    public static class AnyThrow
    {
        public static RuntimeException unchecked( Throwable e )
        {
            AnyThrow.<RuntimeException>throwAny( e );
            return null;
        }

        @SuppressWarnings("unchecked")
        private static <E extends Throwable> void throwAny( Throwable e ) throws E
        {
            throw (E) e;
        }
    }

    @Override
    public void stop() throws Exception
    {
        rollback();
        // don't own the db, will be stopped when driver's stopped
    }

    @Override
    public Version getVersion()
    {
        return new Version( org.neo4j.kernel.Version.getKernel().getRevision() );
    }
}
