package org.neo4j.jdbc.internal.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;

/**
 * @author mh
 * @since 12.06.12
 */
public class Resources
{
    private final Client client;

    private ObjectMapper mapper = new ObjectMapper();
    private final Reference ref;
    private String user;
    private String password;

    public Resources( String url, Client client )
    {
        this.client = client;
        ref = new Reference( new Reference( url ), "/" );
    }

    private Context createContext()
    {
        Context context = new Context();
        context.setClientDispatcher( client );
        return context;
    }

    public void setAuth( String user, String password )
    {
        this.user = user;
        this.password = password;
    }

    public DiscoveryClientResource getDiscoveryResource() throws IOException
    {
        DiscoveryClientResource discovery = withAuth( new DiscoveryClientResource( createContext(), ref ) );
        discovery.readInformation();
        return discovery;

    }

    <T extends ClientResource> T withAuth( T resource )
    {
        if ( hasAuth() )
        {
            resource.setChallengeResponse( ChallengeScheme.HTTP_BASIC, user, password );
        }
        return resource;
    }

    private boolean hasAuth()
    {
        return user != null && password != null;
    }

    public ClientResource getCypherResource( String cypherPath )
    {
        return withAuth( new CypherClientResource( new Context(), cypherPath, mapper ) );
    }

    public TransactionClientResource getTransactionResource( String transactionPath )
    {
        return withAuth( new TransactionClientResource( new Context(), transactionPath ) );
    }

    public TransactionClientResource getTransactionResource( Reference transactionPath )
    {
        return withAuth( new TransactionClientResource( new Context(), transactionPath ) );
    }

    public JsonNode readJsonFrom( String uri )
    {
        try
        {
            ClientResource resource = withAuth( new ClientResource( createContext(), uri ) );
            resource.getClientInfo().setAcceptedMediaTypes( streamingJson() );
            return mapper.readTree( resource.get().getReader() );
        }
        catch ( IOException ioe )
        {
            throw new RuntimeException( "Error reading data from URI " + uri );
        }
    }

    private String textField( JsonNode node, String field )
    {
        final JsonNode fieldNode = node.get( field );
        if ( fieldNode == null )
        {
            return null;
        }
        return fieldNode.getTextValue();
    }

    public class DiscoveryClientResource extends ClientResource
    {
        private String version;
        private String cypherPath;
        private String transactionPath;
        private String dataUri;
        private String labelPath;
        private String relationshipTypesPath;
        private String propertyKeysPath;

        public DiscoveryClientResource( Context context, Reference ref )
        {
            super( context, ref );
            getClientInfo().setAcceptedMediaTypes( streamingJson() );
        }

        public String getVersion()
        {
            return version;
        }

        public void readInformation() throws IOException
        {
            // Get service root
            JsonNode discoveryInfo = mapper.readTree( get().getReader() );

            dataUri = textField( discoveryInfo, "data" );

            JsonNode serverData = readJsonFrom( dataUri );

            version = textField( serverData, "neo4j_version" );

            cypherPath = obtainCypherPath( serverData );
            labelPath = dataUri + "/labels"; // serverData.get("labels").asText(); // /db/data/labels
            relationshipTypesPath = serverData.get( "relationship_types" ).asText(); // /db/data/relationship/types
            propertyKeysPath = dataUri + "/propertykeys"; // serverData.get("property_keys").asText(); //
            // /db/data/relationship/types
            transactionPath = textField( serverData, "transaction" );
            if ( transactionPath == null && (version.startsWith( "2" ) || version.equals( "1.9.M02-1083-g0593b83" )) )
            {
                transactionPath = dataUri + "/transaction";
            }
        }

        private String obtainCypherPath( JsonNode serverData )
        {
            String cypherPath = textField( serverData, "cypher" );
            if ( cypherPath == null )
            {
                final JsonNode extensions = serverData.get( "extensions" );
                if ( extensions != null )
                {
                    final JsonNode plugin = extensions.get( "CypherPlugin" );
                    if ( plugin != null )
                    {
                        cypherPath = textField( plugin, "execute_query" );
                    }
                }
            }
            return cypherPath;
        }

        public String getCypherPath()
        {
            return cypherPath;
        }

        public Collection<String> getLabels()
        {
            return readListFrom( labelPath );
        }

        public Collection<String> getRelationshipTypes()
        {
            return readListFrom( relationshipTypesPath );
        }

        public Collection<String> getPropertyKeys()
        {
            return readListFrom( propertyKeysPath );
        }

        private Collection<String> readListFrom( String uri )
        {
            Iterator<JsonNode> it = readJsonFrom( uri ).getElements();
            List<String> result = new ArrayList<>();
            while ( it.hasNext() )
            {
                result.add( it.next().asText() );
            }
            return result;
        }

        public String getTransactionPath()
        {
            return transactionPath;
        }
    }


    private static class CypherClientResource extends ClientResource
    {
        private final ObjectMapper mapper;

        public CypherClientResource( final Context context, String cypherPath, ObjectMapper mapper )
        {
            super( context, cypherPath );
            this.mapper = mapper;
            getClientInfo().setAcceptedMediaTypes( streamingJson() );
        }

        @Override
        public void doError( Status errorStatus )
        {
            try
            {
                JsonNode node = mapper.readTree( getResponse().getEntity().getReader() );
                JsonNode message = node.get( "message" );
                if ( message != null )
                {
                    super.doError( new Status( errorStatus.getCode(), message.toString(), message.toString(),
                            errorStatus.getUri() ) );
                }
            }
            catch ( IOException e )
            {
                // Ignore
            }

            super.doError( errorStatus );
        }
    }

    public TransactionClientResource subResource( TransactionClientResource res, String segment )
    {
        return withAuth( res.subResource( segment ) );
    }

    public static class TransactionClientResource extends ClientResource
    {

        public TransactionClientResource( final Context context, String path )
        {
            super( context, path );
            getClientInfo().setAcceptedMediaTypes( streamingJson() );
        }

        public TransactionClientResource( final Context context, Reference path )
        {
            super( context, path );
            getClientInfo().setAcceptedMediaTypes( streamingJson() );
        }

        public TransactionClientResource subResource( String segment )
        {
            return new TransactionClientResource( getContext(), getReference().clone().addSegment( segment ) );
        }

        @Override
        public void doError( Status errorStatus )
        {
            String errors = getResponse().getEntityAsText();
            if ( errors == null || !errors.isEmpty() )
            {
                super.doError( new Status( errorStatus.getCode(), "Error executing statement", errors,
                        errorStatus.getUri() ) );
            }
            super.doError( errorStatus );
        }

        private Collection<Object> findErrors( JsonParser parser ) throws IOException
        {
            parser.nextToken(); // todo, parser can be anywhere should return to top-level first?
            if ( "results".equals( parser.getCurrentName() ) )
            {
                parser.skipChildren();
                parser.nextToken();
            }
            List<Object> errors = Collections.emptyList();
            if ( "errors".equals( parser.getCurrentName() ) )
            {
                if ( JsonToken.START_ARRAY == parser.nextToken() )
                {
                    errors = parser.readValueAs( new TypeReference<Object>()
                    {
                    } );
                }
            }
            return errors;
        }
    }

    private static List<Preference<MediaType>> streamingJson()
    {
        final MediaType mediaType = streamingJsonType();
        return Collections.singletonList( new Preference<MediaType>( mediaType ) );
    }

    private static MediaType streamingJsonType()
    {
        final Series<Parameter> parameters = new Series<Parameter>( Parameter.class );
        parameters.add( "stream", "true" );
        return new MediaType( MediaType.APPLICATION_JSON.getName(), parameters );
    }
}
