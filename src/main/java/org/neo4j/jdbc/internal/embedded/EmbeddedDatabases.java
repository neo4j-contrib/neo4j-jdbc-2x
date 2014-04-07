package org.neo4j.jdbc.internal.embedded;

import java.util.Properties;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.jdbc.internal.Databases;
import org.neo4j.jdbc.internal.QueryExecutor;
import org.neo4j.test.ImpermanentGraphDatabase;

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
                        return new ImpermanentGraphDatabase();
                    }
                },
        instance
                {
                    @Override
                    public GraphDatabaseService create( String name, Properties properties )
                    {
                        return (GraphDatabaseService) properties.remove( name );
                    }
                },
        file
                {
                    @Override
                    public GraphDatabaseService create( String name, Properties properties )
                    {
                        if ( isReadOnly( properties ) )
                        {
                            return new GraphDatabaseFactory().newEmbeddedDatabaseBuilder( name ).setConfig(
                                    GraphDatabaseSettings.read_only, "true" ).newGraphDatabase();
                        }
                        else
                        {
                            return new GraphDatabaseFactory().newEmbeddedDatabase( name );
                        }
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

    private GraphDatabaseService defaultImpermanentDb()
    {
        return new ImpermanentGraphDatabase();
    }

    public QueryExecutor createExecutor( String connectionUrl, Properties properties )
    {
        GraphDatabaseService gds = createDatabase( connectionUrl, properties );
        return new EmbeddedQueryExecutor( gds );
    }
}
