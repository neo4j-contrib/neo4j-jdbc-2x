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

import java.sql.SQLException;
import java.util.List;

/**
 * ResultSet implementation that is backed by Lists.
 */
public class ListResultSet extends AbstractResultSet
{
    private static final int BEFORE_FIRST = -1;
    private int current = -1;
    private List<List<Object>> data;
    private final int rows;

    public ListResultSet( List<Neo4jColumnMetaData> columns, List<List<Object>> data, Neo4jConnection conn )
    {
        super( columns, conn );
        this.data = data;
        rows = this.data.size();
    }

    @Override
    public boolean next() throws SQLException
    {
        // todo relative(1)
        current++;

        return rows > current;
    }

    @Override
    protected Object[] currentRow()
    {
        return data.get( current ).toArray();
    }

    @Override
    public boolean isBeforeFirst() throws SQLException
    {
        return current == BEFORE_FIRST;
    }

    @Override
    public boolean isAfterLast() throws SQLException
    {
        return current >= rows;
    }

    @Override
    public boolean isFirst() throws SQLException
    {
        return current == 0;
    }

    @Override
    public boolean isLast() throws SQLException
    {
        return current == rows - 1;
    }

    @Override
    public void beforeFirst() throws SQLException
    {
        current = BEFORE_FIRST;
    }

    @Override
    public void afterLast() throws SQLException
    {
        current = rows;
    }

    @Override
    public boolean first() throws SQLException
    {
        current = 0;
        return true;
    }

    @Override
    public boolean last() throws SQLException
    {
        current = rows - 1;
        return true;
    }

    @Override
    public int getRow() throws SQLException
    {
        return current + 1;
    }

    @Override
    public boolean absolute( int i ) throws SQLException
    {
        if ( i > 0 )
        {
            current = i - 1;
        }
        else
        {
            current = rows - i;
        }

        return false;
    }

    @Override
    public boolean relative( int i ) throws SQLException
    {
        if ( current + i >= 0 && current + i < rows )
        {
            current += i;
            return true;
        }
        return false;
    }

    @Override
    public boolean previous() throws SQLException
    {
        return relative( -1 );
    }

    @Override
    public String toString()
    {
        String result = super.toString();
        for ( List<Object> row : data )
        {
            result += "\n" + row;
        }
        return result;
    }

}
