package org.neo4j.jdbc.internal.util;

import java.util.Properties;

public class Uris
{
    public static void parseUrlProperties( String s, Properties properties )
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
}
