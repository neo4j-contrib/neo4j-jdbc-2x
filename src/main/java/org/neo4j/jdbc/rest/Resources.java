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
package org.neo4j.jdbc.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.CharacterSet;
import org.restlet.data.ClientInfo;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;

import static java.util.Arrays.asList;

/**
 * @author mh
 * @since 12.06.12
 */
public class Resources
{
    private final Client client;

    private static ObjectMapper mapper = new ObjectMapper();
    private final Reference ref;
    private String user;
    private String password;
    private final String userAgent;

    public Resources( String url, Client client, String userAgent )
    {
        this.client = client;
        this.userAgent = userAgent;
        ref = new Reference( new Reference( url ), "/" );
    }

    private static void configureClient( Context context, ClientInfo clientInfo )
    {
        context.getLogger().setLevel( Level.WARNING );
        clientInfo.setAcceptedMediaTypes( streamingJson() );
        clientInfo.setAcceptedCharacterSets( charsetUtf8() );
    }

    static Representation toRepresentation( ObjectNode requestData, ClientResource requestResource )
    {
        try
        {
            final String jsonString = toString( requestData );
            final Variant variant = new Variant( MediaType.APPLICATION_JSON );
            variant.setCharacterSet( CharacterSet.UTF_8 );
            Representation representation = requestResource.toRepresentation( jsonString, variant );
            representation.setCharacterSet( CharacterSet.UTF_8 );
            return representation;
        } catch (IOException ioe) {
            throw new RuntimeException( "Cant convert to representation with UTF-8" , ioe);
        }
    }

    private static String toString( Object value )
    {
        if ( value == null )
        {
            return null;
        }
        return value.toString();
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
        DiscoveryClientResource discovery = withAuth( new DiscoveryClientResource( createContext(), ref, userAgent ) );
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
        return withAuth( new CypherClientResource( new Context(), cypherPath, mapper, userAgent ) );
    }

    public TransactionClientResource getTransactionResource( String transactionPath )
    {
        return withAuth( new TransactionClientResource( new Context(), transactionPath, userAgent ) );
    }

    public TransactionClientResource getTransactionResource( Reference transactionPath )
    {
        return withAuth( new TransactionClientResource( new Context(), transactionPath, userAgent ) );
    }

    public JsonNode readJsonFrom( String uri )
    {
        try
        {
            Context context = createContext();
            ClientResource resource = withAuth( new ClientResource( context, uri ) );

            configureClient( context, resource.getClientInfo() );
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

    public static abstract class Neo4jClientResource extends ClientResource
    {

        public Neo4jClientResource( Context context, Reference ref, String userAgent )
        {
            super( context, ref );
            configureClient( context, getClientInfo() );
            getClientInfo().setAgent( userAgent );
        }

        public Neo4jClientResource( Context context, String uri, String userAgent )
        {
            super(context, uri);
            configureClient( context, getClientInfo() );
            getClientInfo().setAgent( userAgent );
        }

        @Override
        public final Representation toRepresentation( Object source, Variant target ) throws IOException
        {
            target.setCharacterSet( CharacterSet.UTF_8 );
            Representation representation = super.toRepresentation( source, target );
            representation.setCharacterSet( CharacterSet.UTF_8 );
            return representation;
        }
    }

    public class DiscoveryClientResource extends Neo4jClientResource
    {
        private String version;
        private String transactionPath;
        private String dataUri;
        private String labelPath;
        private String relationshipTypesPath;
        private String propertyKeysPath;

        public DiscoveryClientResource( Context context, Reference ref, String userAgent )
        {
            super(context, ref, userAgent);
            configureClient( context, getClientInfo() );
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

            labelPath = serverData.get("node_labels").asText();
            relationshipTypesPath = serverData.get( "relationship_types" ).asText();
            propertyKeysPath = dataUri + "propertykeys";
            // /db/data/relationship/types
            transactionPath = textField( serverData, "transaction" );
            if ( transactionPath == null && version.startsWith( "2"))
            {
                transactionPath = dataUri + "transaction";
            }
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


    private static class CypherClientResource extends Neo4jClientResource
    {
        private final ObjectMapper mapper;

        public CypherClientResource( final Context context, String cypherPath, ObjectMapper mapper, String userAgent )
        {
            super( context, cypherPath, userAgent );
            this.mapper = mapper;
            configureClient( context, getClientInfo() );
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

    public static class TransactionClientResource extends Neo4jClientResource
    {

        private final String userAgent;

        public TransactionClientResource( final Context context, String path, String userAgent )
        {
            super( context, path, userAgent );
            this.userAgent = userAgent;
            configureClient( context, getClientInfo() );
        }

        public TransactionClientResource( final Context context, Reference path, String userAgent )
        {
            super( context, path, userAgent );
            this.userAgent = userAgent;
            configureClient( context, getClientInfo() );
        }

        public TransactionClientResource subResource( String segment )
        {
            return new TransactionClientResource( getContext(), getReference().clone().addSegment( segment ), userAgent );
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

    private static List<Preference<CharacterSet>> charsetUtf8()
    {
        return asList ( new Preference<>( CharacterSet.UTF_8 ));
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
