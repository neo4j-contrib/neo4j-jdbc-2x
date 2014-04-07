/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.neo4j.jdbc.internal;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.jdbc.Driver;
import org.neo4j.jdbc.Version;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;

import org.neo4j.cypherdsl.CypherQuery;
import org.neo4j.cypherdsl.expression.Expression;
import org.neo4j.cypherdsl.grammar.Execute;
import org.neo4j.cypherdsl.grammar.ExecuteWithParameters;
import org.neo4j.jdbc.internal.rest.Resources;
import org.neo4j.jdbc.internal.rest.RestQueryExecutor;
import org.neo4j.jdbc.internal.rest.TransactionalQueryExecutor;

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

    public Neo4jConnection( Driver driver, String jdbcUrl, Properties properties ) throws SQLException
    {
        this.driver = driver;
        this.url = jdbcUrl;
        this.properties.putAll( properties );
        this.debug = hasDebug();
        final String connectionUrl = jdbcUrl.substring( Driver.URL_PREFIX.length() );
        this.queryExecutor = createExecutor( connectionUrl, getUser(), getPassword(), properties );
        this.version = this.queryExecutor.getVersion();
        validateVersion();
    }

    private QueryExecutor createExecutor( String connectionUrl, String user, String password,
                                          Properties properties ) throws SQLException
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

            Resources resources = new Resources( remoteUrl, client );

            if ( user != null && password != null )
            {
                resources.setAuth( user, password );
            }

            try
            {
                Resources.DiscoveryClientResource discovery = resources.getDiscoveryResource();

                if ( !properties.containsKey( Driver.LEGACY ) && discovery.getTransactionPath() != null )
                {
                    return new TransactionalQueryExecutor( resources );
                }
                else if ( discovery.getCypherPath() != null )
                {
                    url = connectionUrl;

                    return new RestQueryExecutor( resources );
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

    @Override
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

    @Override
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

    @Override
    public boolean isClosed() throws SQLException
    {
        return closed;
    }

    @Override
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

    // Connection helpers
    public ResultSet executeQuery( Execute execute ) throws SQLException
    {
        if ( execute instanceof ExecuteWithParameters )
        {
            return executeQuery( execute.toString(), ((ExecuteWithParameters) execute).getParameters() );
        }
        else
        {
            return executeQuery( execute.toString(), Collections.<String, Object>emptyMap() );
        }
    }

    public ResultSet executeQuery( final String query, Map<String, Object> parameters ) throws SQLException
    {
        checkClosed( "execute" );
        checkReadOnly( query );
        try
        {
            if ( log.isDebugEnabled() )
            {
                log.info( "Executing query: " + query + "\n with params " + parameters );
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
        ResultSet columns = executeQuery( driver.getQueries().getColumns( tableName ) );
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

    public Iterable<Expression> returnProperties( String tableName, String columnPrefix ) throws SQLException
    {
        ResultSet columns = executeQuery( driver.getQueries().getColumns( tableName ) );
        List<Expression> properties = new ArrayList<Expression>();
        while ( columns.next() )
        {
            properties.add( CypherQuery.identifier( columnPrefix ).property( columns.getString( "property.name" ) ) );
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
