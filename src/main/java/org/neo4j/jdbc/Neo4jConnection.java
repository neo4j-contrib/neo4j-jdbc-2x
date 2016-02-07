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
package org.neo4j.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.jdbc.rest.Resources;
import org.neo4j.jdbc.rest.TransactionalQueryExecutor;
import org.neo4j.jdbc.util.UserAgentBuilder;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;

import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Implementation of Connection that delegates to the Neo4j REST API and sends queries as Cypher requests
 */
public class Neo4jConnection extends AbstractConnection
{
    protected final static Log log = LogFactory.getLog( Neo4jConnection.class );

    private String url;

    private QueryExecutor queryExecutor;
    private boolean closed = false;
    private final Properties properties = new Properties();
    private boolean debug;
    private Driver driver;
    private Version version;

    private SQLWarning sqlWarnings;
    private boolean readonly = false;
    private boolean autoCommit = true;
    private final UserAgentBuilder userAgentBuilder;

    public Neo4jConnection( Driver driver, String jdbcUrl, Properties properties ) throws SQLException
    {
        this.driver = driver;
        this.url = jdbcUrl;
        this.properties.putAll( properties );
        this.debug = hasDebug();
        this.userAgentBuilder = new UserAgentBuilder( properties );
        final String connectionUrl = extractConnectionUrl( jdbcUrl );

        this.queryExecutor = createExecutor( connectionUrl, getUser(), getPassword(), userAgentBuilder.getAgent() );
        this.version = this.queryExecutor.getVersion();
        validateVersion();
    }

    private static String extractConnectionUrl( String jdbcUrl ) {
        if ( jdbcUrl == null ) throw new IllegalArgumentException( "JDBC URL " +
                "must not be null" );

        if ( jdbcUrl.isEmpty() ) throw new IllegalArgumentException( "JDBC " +
                "URL must not be empty" );

        final String baseUrl = jdbcUrl.substring( Driver.URL_PREFIX.length() );

        if (baseUrl.isEmpty()) throw new IllegalArgumentException( "JDBC URL " +
                "must specify a server" );

        /* When we chop off the URL_PREFIX and the next item is a http(s)://
           protocol indicator, we can be left with an extra colon that will
           cause the parsing of the http based url to fail. */
        if ( baseUrl.startsWith(":http") ) {
            return baseUrl.substring(1);
        } else {
            return baseUrl;
        }
    }

    private QueryExecutor createExecutor( String connectionUrl, String user, String password,
                                          String userAgent ) throws SQLException
    {
        if ( connectionUrl.contains( "://" ) )
        {
            String remoteUrl = connectionUrl;
            if (!(connectionUrl.startsWith( "http://" ) || connectionUrl.startsWith( "https://" )))
                remoteUrl = "http"+remoteUrl; // Default to HTTP if not specified

            if ( log.isDebugEnabled() )
            {
                log.debug( "Connecting to URL " + url );
            }

            Client client = new Client( new Context(), Arrays.asList( Protocol.valueOf(remoteUrl.split( ":" )[0]) ), properties.getProperty( "restlet.helperclass" ) );

            Resources resources = new Resources( remoteUrl, client, userAgent );

            if ( user != null && password != null )
            {
                resources.setAuth( user, password );
            }

            try
            {
                Resources.DiscoveryClientResource discovery = resources.getDiscoveryResource();

                if (discovery.getTransactionPath() != null )
                {
                    return new TransactionalQueryExecutor( resources );
                }
                throw new SQLException( "Could not connect to the Neo4j Server at " + remoteUrl + " " + discovery
                        .getVersion() );
            }
            catch ( IOException e )
            {
                throw new SQLException( "Error connecting to Neo4j Server at " + connectionUrl, e );
            }
        }

        return getDriver().createExecutor( connectionUrl, properties );
    }

    private String getPassword()
    {
        return properties.getProperty( Driver.PASSWORD );
    }

    private String getUser()
    {
        return properties.getProperty( Driver.USER );
    }

    private boolean hasAuth()
    {
        return properties.contains( Driver.USER ) && properties.contains( Driver.PASSWORD );
    }

    private boolean hasDebug()
    {
        return Connections.hasDebug( properties );
    }

    private void validateVersion() throws SQLException
    {
        if ( version.getMajorVersion() < 1 ||
                version.getMajorVersion() == 1 && version.getMinorVersion() < 5 )
        {
            throw new SQLException( "Unsupported Neo4j version:" + version );
        }
    }

    public Statement createStatement() throws SQLException
    {
        return debug( new Neo4jStatement( this ) );
    }

    public PreparedStatement prepareStatement( String statement ) throws SQLException
    {
        return debug( new Neo4jPreparedStatement( this, statement ) );
    }

    @Override
    public void setAutoCommit( boolean autoCommit ) throws SQLException
    {
        if ( autoCommit == this.autoCommit )
        {
            return;
        }
        if ( autoCommit )
        {
            commit();
        }
        this.autoCommit = autoCommit;
    }

    @Override
    public boolean getAutoCommit() throws SQLException
    {
        return autoCommit;
    }

    @Override
    public void commit() throws SQLException
    {
        checkClosed( "commit" );
        if ( autoCommit )
        {
            throw new SQLException( "Commit called on auto-committed connection" );
        }
        try
        {
            queryExecutor.commit();
        }
        catch ( SQLException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new SQLException( "Error during commit", e );
        }
    }

    @Override
    public void rollback() throws SQLException
    {
        checkClosed( "rollback" );

        if ( getAutoCommit() )
        {
            throw new SQLException( "Rollback called on auto-committed connection" );
        }
        try
        {
            queryExecutor.rollback();
        }
        catch ( SQLException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new SQLException( "Error during commit", e );
        }
    }

    public void close() throws SQLException
    {
        checkClosed( "close" );
        try
        {
            queryExecutor.rollback();
            queryExecutor.stop();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        finally
        {
            closed = true;
        }

    }

    public boolean isClosed() throws SQLException
    {
        return closed;
    }

    public DatabaseMetaData getMetaData() throws SQLException
    {
        return debug( new Neo4jDatabaseMetaData( this ) );
    }

    @Override
    public void setReadOnly( boolean readOnly ) throws SQLException
    {
        this.readonly = readOnly;
    }

    @Override
    public boolean isReadOnly() throws SQLException
    {
        return readonly;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException
    {
        return sqlWarnings;
    }

    @Override
    public void clearWarnings() throws SQLException
    {
        sqlWarnings = null;
    }

    @Override
    public Statement createStatement( int resultSetType, int resultSetConcurrency ) throws SQLException
    {
        return debug( new Neo4jStatement( this ) );
    }

    @Override
    public PreparedStatement prepareStatement( String sql, int resultSetType, int resultSetConcurrency ) throws
            SQLException
    {
        return debug( new Neo4jPreparedStatement( this, nativeSQL( sql ) ) );
    }


    @Override
    public Statement createStatement( int resultSetType, int resultSetConcurrency, int resultSetHoldability ) throws
            SQLException
    {
        return debug( new Neo4jStatement( this ) );
    }

    @Override
    public PreparedStatement prepareStatement( String sql, int resultSetType, int resultSetConcurrency,
                                               int resultSetHoldability ) throws SQLException
    {
        return debug( new Neo4jPreparedStatement( this, nativeSQL( sql ) ) );
    }

    @Override
    public PreparedStatement prepareStatement( String sql, int autoGeneratedKeys ) throws SQLException
    {
        return debug( new Neo4jPreparedStatement( this, nativeSQL( sql ) ) );
    }

    @Override
    public PreparedStatement prepareStatement( String sql, int[] columnIndexes ) throws SQLException
    {
        return debug( new Neo4jPreparedStatement( this, nativeSQL( sql ) ) );
    }

    @Override
    public PreparedStatement prepareStatement( String sql, String[] columnNames ) throws SQLException
    {
        return debug( new Neo4jPreparedStatement( this, nativeSQL( sql ) ) );
    }

    @Override
    public boolean isValid( int timeout ) throws SQLException
    {
        return true;
    }

    public ResultSet executeQuery( final String query, Map<String, Object> parameters ) throws SQLException
    {
        checkClosed( "execute" );
        checkReadOnly( query );
        try
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Executing query: " + query + "\n with params " + parameters );
            }
            final ExecutionResult result = queryExecutor.executeQuery( query, parameters, autoCommit );
            return debug( toResultSet( result ) );
        }
        catch ( SQLException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new SQLException( "Error executing query " + query + "\n with params " + parameters, e );
        }
    }

    private void checkClosed( String method ) throws SQLException
    {
        if ( isClosed() )
        {
            throw new SQLException( method + " called on closed connection." );
        }
    }

    private void checkReadOnly( String query ) throws SQLException
    {
        if ( readonly && isMutating( query ) )
        {
            throw new SQLException( "Mutating Query in readonly mode: " + query );
        }
    }

    private boolean isMutating( String query )
    {
        return query.matches( "(?is).*\\b(create|relate|delete|set)\\b.*" );
    }


    public String tableColumns( String tableName, String columnPrefix ) throws SQLException
    {
        ResultSet columns = executeQuery( driver.getQueries().getColumns( tableName ), Collections.<String,
                Object>singletonMap( "typeName", tableName ));
        StringBuilder columnsBuilder = new StringBuilder();
        while ( columns.next() )
        {
            if ( columnsBuilder.length() > 0 )
            {
                columnsBuilder.append( ',' );
            }
            columnsBuilder.append( columnPrefix ).append( columns.getString( "property.name" ) );
        }
        return columnsBuilder.toString();
    }

    public Iterable<String> returnProperties( String tableName, String columnPrefix ) throws SQLException
    {
        ResultSet columns = executeQuery( driver.getQueries().getColumns( tableName ),  Collections.<String, Object>singletonMap( "typeName", tableName ) );
        List<String> properties = new ArrayList<String>();
        while ( columns.next() )
        {
            properties.add(  columnPrefix + columns.getString( "property.name" ) );
        }
        return properties;
    }

    String getURL()
    {
        return url;
    }

    protected ResultSet toResultSet( ExecutionResult result ) throws SQLException
    {
        return new IteratorResultSet( this, result.columns(), result.getResult() );
    }

    public <T> T debug( T obj )
    {
        return Connections.debug( obj, debug );
    }

    public Properties getProperties()
    {
        return properties;
    }

    public Driver getDriver()
    {
        return driver;
    }

    public Version getVersion()
    {
        return version;
    }
}
