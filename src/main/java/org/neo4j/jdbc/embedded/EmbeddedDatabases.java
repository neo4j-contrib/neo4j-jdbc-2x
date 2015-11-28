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

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.jdbc.Databases;
import org.neo4j.jdbc.QueryExecutor;
import org.neo4j.test.TestGraphDatabaseFactory;

/**
 * @author mh
 * @since 15.06.12
 */
public class EmbeddedDatabases implements Databases
{
    private static final Pattern urlMatcher = Pattern.compile( ":([^:]*):(.+)" );

    enum Type
    {
        mem
                {
                    @Override
                    public GraphDatabaseService create( String name, Properties properties )
                    {
                        return withShutdownHook( defaultImpermanentDb() );
                    }
                }, instance
            {
                @Override
                public GraphDatabaseService create( String name, Properties properties )
                {
                    return (GraphDatabaseService) properties.remove( name );
                }
            }, file
            {
                @Override
                public GraphDatabaseService create( final String name, Properties properties )
                {
                    GraphDatabaseBuilder builder = new GraphDatabaseBuilder(new GraphDatabaseBuilder.DatabaseCreator() {
                        @Override
                        public GraphDatabaseService newDatabase(Map<String, String> map) {
                            return new GraphDatabaseFactory().newEmbeddedDatabase(new File(name));
                        }
                    });

                    if ( isReadOnly( properties ) )
                    {
                        builder.setConfig(GraphDatabaseSettings.read_only,"true");

                    }
                    return withShutdownHook( builder.newGraphDatabase() );
                }
            };

        public abstract GraphDatabaseService create( String name, Properties properties );

        protected boolean isReadOnly( Properties properties )
        {
            return properties != null && properties.getProperty( "readonly", "false" ).equalsIgnoreCase( "true" );
        }
    }

    private final WeakHashMap<String, GraphDatabaseService> databases = new WeakHashMap<String, GraphDatabaseService>();

    public GraphDatabaseService createDatabase( String connectionUrl, Properties properties )
    {
        Matcher matcher = urlMatcher.matcher( connectionUrl );
        if ( !matcher.find() )
        {
            return defaultImpermanentDb();
        }
        try
        {
            Type type = Type.valueOf( matcher.group( 1 ) );
            String name = matcher.group( 2 );
            GraphDatabaseService gds = databases.get( name );
            if ( gds != null )
            {
                return gds;
            }
            synchronized ( urlMatcher )
            {
                gds = databases.get( name );
                if ( gds != null )
                {
                    return gds;
                }
                gds = type.create( name, properties );
                databases.put( name, gds );
            }
            return gds;
        }
        catch ( IllegalArgumentException e )
        {
            return defaultImpermanentDb();
        }
    }

    private static GraphDatabaseService defaultImpermanentDb()
    {
        return new TestGraphDatabaseFactory().newImpermanentDatabase();
    }

    private static GraphDatabaseService withShutdownHook( final GraphDatabaseService db )
    {
        Runtime.getRuntime().addShutdownHook( new Thread(  ) {
            @Override
            public void run()
            {
                db.shutdown();
            }
        });
        return db;
    }

    public QueryExecutor createExecutor( String connectionUrl, Properties properties )
    {
        GraphDatabaseService gds = createDatabase( connectionUrl, properties );
        return new EmbeddedQueryExecutor( gds );
    }
}
