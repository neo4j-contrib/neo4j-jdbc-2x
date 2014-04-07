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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Helper to build ResultSets using a fluent DSL approach.
 */
public class ResultSetBuilder
{
    private List<Neo4jColumnMetaData> columns = new ArrayList<Neo4jColumnMetaData>();

    private List<List<Object>> data = new ArrayList<List<Object>>();

    private List<Object> currentRow = new ArrayList<Object>();

    public ResultSetBuilder column( String name, int type )
    {
        String typeName = null;
        if ( type == Types.VARCHAR )
        {
            typeName = String.class.getName();
        }
        else if ( type == Types.INTEGER )
        {
            typeName = Integer.class.getName();
        }

        columns.add( new Neo4jColumnMetaData( name, typeName, type ) );
        return this;
    }

    public ResultSetBuilder column( String name )
    {
        return column( name, Types.VARCHAR );
    }

    public ResultSetBuilder rowData( Collection<Object> values )
    {
        currentRow = new ArrayList<Object>();
        currentRow.addAll( values );

        for ( int i = currentRow.size(); i < columns.size(); i++ )
        {
            currentRow.add( null );
        }

        data.add( currentRow );
        return this;
    }

    public ResultSetBuilder row( Object... values )
    {
        return rowData( Arrays.asList( values ) );
    }

    public ResultSetBuilder cell( String name, Object value )
    {
        int i = getColumnIndex( name );
        if ( i == -1 )
        {
            throw new IllegalArgumentException( "No such column declared:" + name );
        }
        currentRow.set( i, value );
        return this;
    }

    public ResultSet newResultSet( Connection connection ) throws SQLException
    {
        return new ListResultSet( columns, data, connection.unwrap( Neo4jConnection.class ) );
    }

    private int getColumnIndex( String name )
    {
        for ( int i = 0; i < columns.size(); i++ )
        {
            Neo4jColumnMetaData columnMetaData = columns.get( i );
            if ( columnMetaData.getName().equals( name ) )
            {
                return i;
            }
        }
        return -1;
    }
}
