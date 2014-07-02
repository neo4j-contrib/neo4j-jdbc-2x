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
package org.neo4j.jdbc.util;

import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.helpers.collection.IteratorUtil;

import java.sql.SQLException;
import java.util.*;

public class PropertyContainerMap extends AbstractMap<String, Object> implements Castable
{
    private final PropertyContainer source;

    public PropertyContainerMap( PropertyContainer source )
    {
        this.source = source;
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


    @Override
    public String toString()
    {
        return source.toString();
    }


    @Override
    public Set<Entry<String, Object>> entrySet()
    {
        return new AbstractSet<Entry<String, Object>>()
        {
            @Override
            public Iterator<Entry<String, Object>> iterator()
            {
                return new PropertyIterator( source );
            }

            @Override
            public int size()
            {
                return IteratorUtil.count( source.getPropertyKeys() );
            }
        };
    }

    private static class PropertyIterator implements Iterator<Entry<String, Object>>
    {

        private final PropertyContainer source;
        private final Iterator<String> keys;

        public PropertyIterator( PropertyContainer source )
        {
            this.source = source;
            this.keys = source.getPropertyKeys().iterator();
        }

        @Override
        public boolean hasNext()
        {
            return keys.hasNext();
        }

        @Override
        public Entry<String, Object> next()
        {
            final String key = keys.next();
            return new Entry<String, Object>()
            {
                @Override
                public String getKey()
                {
                    return key;
                }

                @Override
                public Object getValue()
                {
                    return source.getProperty( key );
                }

                @Override
                public Object setValue( Object value )
                {
                    Object old = source.getProperty( key, null );
                    source.setProperty( key, value );
                    return old;
                }
            };
        }

        @Override
        public void remove()
        {
            keys.remove();
        }
    }
}
