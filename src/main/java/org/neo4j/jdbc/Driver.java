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

import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.restlet.Context;

/**
 * JDBC Driver implementation that is backed by a REST Neo4j Server.
 */
public class Driver implements java.sql.Driver
{
    private final static Log log = LogFactory.getLog( Driver.class );
    public static final String CON_PREFIX = "jdbc:neo4j:";

    static
    {
        try
        {
            DriverManager.registerDriver( new Driver() );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }
    }

    static final String URL_PREFIX = "jdbc:neo4j";
    static final String PASSWORD = "password";
    static final String USER = "user";

    DriverQueries queries;

    public Driver()
    {
        queries = new DriverQueries();
    }

    public Neo4jConnection connect( String url, Properties properties ) throws SQLException
    {
        if ( !acceptsURL( url ) )
        {
            return null;
        }
        parseUrlProperties( url, properties );

        return Connections.create( this, url, properties );
    }

    public boolean acceptsURL( String s ) throws SQLException
    {
        return s.startsWith( CON_PREFIX );
    }

    public DriverPropertyInfo[] getPropertyInfo( String s, Properties props ) throws SQLException
    {
        return new DriverPropertyInfo[]
                {
                        infoFor( props, "debug" ),
                        infoFor( props, "user" ),
                        infoFor( props, "password" )
                };
    }

    private DriverPropertyInfo infoFor( Properties properties, String name )
    {
        return new DriverPropertyInfo( name, properties.getProperty( name ) );
    }

    public int getMajorVersion()
    {
        return 1;
    }

    public int getMinorVersion()
    {
        return 0;
    }

    public boolean jdbcCompliant()
    {
        return false;
    }

    public DriverQueries getQueries()
    {
        return queries;
    }

    void parseUrlProperties( String s, Properties properties )
    {
        if ( s.contains( "?" ) )
        {
            String urlProps = s.substring( s.indexOf( '?' ) + 1 );
            String[] props = urlProps.split( "," );
            for ( String prop : props )
            {
                int idx = prop.indexOf( '=' );
                if ( idx != -1 )
                {
                    String key = prop.substring( 0, idx );
                    String value = prop.substring( idx + 1 );
                    properties.put( key, value );
                }
                else
                {
                    properties.put( prop, "true" );
                }
            }
        }
    }

    private final Databases databases = createDatabases();

    private Databases createDatabases()
    {
        try
        {
            return (Databases) Class.forName( "org.neo4j.jdbc.embedded.EmbeddedDatabases" ).newInstance();
        }
        catch ( Throwable e )
        {
            log.debug( "Embedded Neo4j support not enabled " + e.getMessage() );
            return null;
        }
    }

    public QueryExecutor createExecutor( String connectionUrl, Properties properties ) throws SQLException
    {
        if ( databases == null )
        {
            throw new SQLFeatureNotSupportedException( "Embedded Neo4j not available please add neo4j-kernel, " +
                    "-index and -cypher to the classpath" );
        }
        return databases.createExecutor( connectionUrl, properties );
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        return null;
    }
}
