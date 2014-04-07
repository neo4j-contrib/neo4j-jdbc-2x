package org.neo4j.jdbc.internal.embedded;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.Exceptions;
import org.neo4j.helpers.collection.IteratorWrapper;
import org.neo4j.jdbc.internal.ExecutionResult;
import org.neo4j.jdbc.internal.QueryExecutor;
import org.neo4j.jdbc.Version;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.KernelData;

/**
 * @author mh
 * @since 15.06.12
 */
public class EmbeddedQueryExecutor implements QueryExecutor
{
    private final ExecutionEngine executionEngine;
    private final GraphDatabaseService gds;
    private final ThreadLocal<Transaction> tx = new ThreadLocal<Transaction>();

    public EmbeddedQueryExecutor( GraphDatabaseService gds )
    {
        this.gds = gds;
        executionEngine = new ExecutionEngine( gds );
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
        if ( !iterator.hasNext() )
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
        transaction.finish();
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
        return new Version( ((GraphDatabaseAPI) gds).getDependencyResolver().resolveDependency( KernelData.class ).version().getRevision() );
    }
}
