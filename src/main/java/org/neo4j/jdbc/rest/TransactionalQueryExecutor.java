package org.neo4j.jdbc.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.neo4j.jdbc.ExecutionResult;
import org.neo4j.jdbc.QueryExecutor;
import org.neo4j.jdbc.Version;
import org.restlet.Response;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Filter;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * @author mh
 * @since 15.06.12
 */
public class TransactionalQueryExecutor implements QueryExecutor
{
    protected final static Log log = LogFactory.getLog( TransactionalQueryExecutor.class );
    private static final Statement[] NO_STATEMENTS = new Statement[0];
    private static final Iterator<ExecutionResult> NO_RESULTS = Collections.<ExecutionResult>emptyList().iterator();
    private final Resources.TransactionClientResource commitResource;

    private final Resources.TransactionClientResource txResource;
    private final ThreadLocal<Resources.TransactionClientResource> transaction = new ThreadLocal<Resources
            .TransactionClientResource>();

    private final ObjectMapper mapper = new ObjectMapper();
    private final Version version;
    private final Resources resources;
    private final StreamingParser resultParser;
    private final Resources.DiscoveryClientResource discovery;

    public TransactionalQueryExecutor( Resources resources ) throws SQLException
    {
        try
        {
            this.resources = resources;

            resultParser = new StreamingParser( mapper );

            discovery = resources.getDiscoveryResource();

            version = new Version( discovery.getVersion() );

            String transactionPath = discovery.getTransactionPath();

            txResource = resources.getTransactionResource( transactionPath );
            commitResource = resources.subResource( txResource, "commit" );
        }
        catch ( IOException e )
        {
            throw new SQLNonTransientConnectionException( e );
        }
    }

    public Iterator<ExecutionResult> begin( Statement... statements ) throws SQLException
    {
        // if (transaction!=null) throw new SQLException("Already in transaction "+transaction);
        final Resources.TransactionClientResource resource = hasActiveTransaction() ? activeTransaction() : txResource;
        Response result = post( resource, statements );
        if ( result.getLocationRef() != null )
        {
            this.transaction.set( resources.getTransactionResource( result.getLocationRef() ) );
        }
        return toResults( result.getEntity(), statements );
    }

    private Resources.TransactionClientResource activeTransaction()
    {
        return transaction.get();
    }

    private boolean hasActiveTransaction()
    {
        return activeTransaction() != null;
    }

    private Response post( Resources.TransactionClientResource resource, Statement[] data )
    {
        final ObjectNode requestData = mapper.createObjectNode();
        requestData.put( "statements", Statement.toJson( mapper, data ) );
        resource.post( toRepresentation( requestData, resource ) );
        Response response = resource.getResponse();
        response.getEntity().setCharacterSet( CharacterSet.UTF_8 );
//        dump( response );
        return response;
    }

    private void dump( Response response )
    {
        System.out.println( "response.getText() = " + response.getAttributes() );
        System.out.println( "response.getText() = " + response.getEntityAsText() );
    }

    private Representation toRepresentation( ObjectNode requestData, ClientResource requestResource )
    {
        final String jsonString = toString( requestData );
        final Variant variant = new Variant( MediaType.APPLICATION_JSON );
        variant.setCharacterSet( CharacterSet.UTF_8 );
        try {
            Representation representation = requestResource.toRepresentation(jsonString, variant);
            representation.setCharacterSet(CharacterSet.UTF_8);
            return representation;
        } catch (IOException ioe) {
            throw new RuntimeException( "Error converting representation", ioe );
        }
    }

    public Iterator<ExecutionResult> commit( Statement... statements ) throws SQLException
    {
        final boolean hasActiveTransaction = hasActiveTransaction();
        if ( (statements == null || statements.length == 0) && !hasActiveTransaction )
        {
            return NO_RESULTS; //throw new SQLException("Not in transaction");
        }
        Resources.TransactionClientResource resource = hasActiveTransaction ? resources.subResource(
                activeTransaction(), "commit" ) : commitResource;
        Representation result = post( resource, statements ).getEntity();
        clearTransaction();
        if ( result.isAvailable() )
        {
            return toResults( result, statements );
        }
        return NO_RESULTS;
        // throw new IllegalStateException("No results for commit");
    }

    private void clearTransaction()
    {
        this.transaction.set( null );
    }

    private Iterator<ExecutionResult> toResults( final Representation result, Statement[] statements ) throws
            SQLException
    {
        final Reader reader = getReader( result );
        AutoCloseable closeable = new AutoCloseable()
        {
            public void close() throws Exception
            {
                reader.close();
            }
        };
        return resultParser.toResults( resultParser.obtainParser( reader ), closeable, statements );
    }

    private Reader getReader( Representation result )
    {
        try
        {
            return result.getReader();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Error accessing response reader", e );
        }
    }

    private String toString( Object value )
    {
        if ( value == null )
        {
            return null;
        }
        return value.toString();
    }

    public void rollback() throws SQLException
    {
        if ( hasActiveTransaction() )
        {
            final Resources.TransactionClientResource resource = activeTransaction();
            resource.delete();
            if ( resource.getResponse().isEntityAvailable() )
            {
                clearTransaction();
            }
        }
    }

    public Iterator<ExecutionResult> executeQueries( Statement... statements ) throws Exception
    {
        if ( hasActiveTransaction() )
        {
            final Representation result = post( activeTransaction(), statements ).getEntity();
            return toResults( result, statements );
        }
        else
        {
            return commit( statements );
        }
    }

    public ExecutionResult executeQuery( String query, Map<String, Object> parameters,
                                         boolean autoCommit ) throws Exception
    {
        try
        {
            final Statement statement = new Statement( query, parameters );
            final Iterator<ExecutionResult> res = autoCommit ? executeQueries( statement ) : begin( statement );
            if ( res.hasNext() )
            {
                final ExecutionResult result = res.next();
                // if (res.hasNext()) throw new SQLException("A single statement resulted in two result sets
                // "+statement.toString());
                return result;
            }
            return ExecutionResult.EMPTY_RESULT; // or throw Exception

        }
        catch ( ResourceException e )
        {
            throw new SQLException( e.getStatus().getReasonPhrase(), e );
        }
    }

    @Override
    public void stop() throws Exception
    {
        ((Filter) txResource.getNext()).stop();
    }

    @Override
    public Version getVersion()
    {
        return version;
    }

    @Override
    public void commit() throws SQLException
    {
        commit( NO_STATEMENTS );
    }


}
