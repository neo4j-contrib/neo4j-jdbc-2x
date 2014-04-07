package org.neo4j.jdbc.internal.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.restlet.Response;
import org.restlet.data.CharacterSet;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Filter;

import org.neo4j.jdbc.internal.ExecutionResult;
import org.neo4j.jdbc.internal.QueryExecutor;
import org.neo4j.jdbc.Version;

/**
 * @author mh
 * @since 15.06.12
 */
public class RestQueryExecutor implements QueryExecutor
{
    protected final static Log log = LogFactory.getLog( RestQueryExecutor.class );

    private ClientResource cypherResource;
    private ObjectMapper mapper = new ObjectMapper();
    private Version version;
    private final Resources.DiscoveryClientResource discovery;

    public RestQueryExecutor( Resources resources ) throws SQLException
    {
        try
        {
            discovery = resources.getDiscoveryResource();

            version = new Version( discovery.getVersion() );

            String cypherPath = discovery.getCypherPath();

            cypherResource = resources.getCypherResource( cypherPath );
        }
        catch ( IOException e )
        {
            throw new SQLNonTransientConnectionException( e );
        }
    }

    public ExecutionResult executeQuery( String query, Map<String, Object> parameters,
                                         boolean autoCommit ) throws Exception
    {
        if ( !autoCommit )
        {
            throw new SQLException( "Manual commit mode not supported over REST" );
        }
        ClientResource resource = null;
        try
        {
            ObjectNode queryNode = queryParameter( query, parameters );

            resource = new ClientResource( cypherResource );
            Representation rep = resource.post( queryNode.toString() );
            rep.setCharacterSet( new CharacterSet( "UTF-8" ) );
            JsonNode node = mapper.readTree( rep.getReader() );
            final ResultParser parser = new ResultParser( node );
            return new ExecutionResult( parser.getColumns(), parser.streamData() );
        }
        catch ( ResourceException e )
        {
            String msg = extractErrorMessage( resource );
            if ( msg != null )
            {
                throw new SQLException( msg, e );
            }
            throw new SQLException( e.getStatus().getReasonPhrase(), e );
        }
        catch ( JsonProcessingException e )
        {
            throw new SQLException( e );
        }
        catch ( IOException e )
        {
            throw new SQLException( e );
        }
    }

    /**
     * When a REST error occurs, the JSON can contain an error message
     */
    private String extractErrorMessage( ClientResource resource )
    {
        try
        {
            if ( resource == null )
            {
                return null;
            }
            Response resp = resource.getResponse();
            if ( resp == null )
            {
                return null;
            }
            Representation rep = resp.getEntity();
            rep.setCharacterSet( new CharacterSet( "UTF-8" ) );

            JsonNode node = mapper.readTree( rep.getReader() );
            if ( node == null )
            {
                return null;
            }
            JsonNode msg = node.findValue( "message" );
            if ( msg == null )
            {
                return null;
            }
            return msg.getTextValue();
        }
        catch ( Exception ex )
        {
            return null;
        }
    }

    @Override
    public void stop() throws Exception
    {
        ((Filter) cypherResource.getNext()).stop();
    }

    @Override
    public Version getVersion()
    {
        return version;
    }

    private ObjectNode queryParameter( String query, Map<String, Object> parameters )
    {
        ObjectNode queryNode = mapper.createObjectNode();
        queryNode.put( "query", escapeQuery( query ) );
        if ( parameters != null )
        {
            queryNode.put( "params", JsonUtils.serialize( parameters, mapper ) );
        }
        return queryNode;
    }

    private String escapeQuery( String query )
    {
        query = query.replace( '\"', '\'' );
        query = query.replace( '\n', ' ' );
        return query;
    }

    @Override
    public void commit() throws Exception
    {
        // no op
    }

    @Override
    public void rollback() throws Exception
    {
        // no op
    }
}
