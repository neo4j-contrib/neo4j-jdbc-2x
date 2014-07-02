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
package org.neo4j.jdbc.embedded;

import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.IteratorWrapper;
import org.neo4j.jdbc.util.Castable;
import org.neo4j.jdbc.util.PropertyContainerMap;

import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Converter
{
    public static Object convert( final Object value )
    {
        if ( value == null )
        {
            return null;
        }
        if ( value instanceof PropertyContainer )
        {
            return new PropertyContainerMap( (PropertyContainer) value );
        }
        if ( value instanceof Map )
        {
            return new GraphElementMap( (Map) value );
        }
        if ( value instanceof Iterable )
        {
            return new GraphIterable( value );
        }
        return value;
    }

    private static class GraphElementMap extends AbstractMap implements Castable
    {
        private final Map source;

        public GraphElementMap( Map source )
        {
            this.source = source;
        }

        @Override
        public Set<Entry> entrySet()
        {
            final Set<Entry> entries = source.entrySet();
            return new AbstractSet<Entry>()
            {
                @Override
                public Iterator<Entry> iterator()
                {
                    return new IteratorWrapper<Entry, Entry>( entries.iterator() )
                    {
                        @Override
                        protected Entry underlyingObjectToObject( Entry entry )
                        {
                            return new SimpleEntry( entry.getKey(),
                                    convert( entry.getValue() ) );
                        }
                    };
                }

                @Override
                public int size()
                {
                    return 0;
                }
            };
        }

        @Override
        public <T> T to( Class<T> type ) throws SQLException
        {
            if ( source == null )
            {
                return null;
            }
            if ( type.isInstance( source ) )
            {
                return type.cast( source );
            }
            throw new SQLException( "Unable to convert from " + source.getClass().getName() + " to " + type );
        }
    }

    private static class GraphIterable extends IterableWrapper
            implements Castable
    {
        private final Object source;

        public GraphIterable( Object value )
        {
            super( (Iterable) value );
            source = value;
        }

        @Override
        protected Object underlyingObjectToObject( Object object )
        {
            return convert( object );
        }

        @Override
        public <T> T to( Class<T> type ) throws SQLException
        {
            if ( source == null )
            {
                return null;
            }
            if ( type.isInstance( source ) )
            {
                return type.cast( source );
            }
            throw new SQLException( "Unable to convert from " + source.getClass().getName() + " to " + type );
        }
    }
}
