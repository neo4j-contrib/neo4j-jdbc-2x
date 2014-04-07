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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * TODO
 */
class Neo4jResultSetMetaData
        implements ResultSetMetaData
{
    private List<Neo4jColumnMetaData> columns;

    Neo4jResultSetMetaData( List<Neo4jColumnMetaData> columns )
    {
        this.columns = columns;
    }

    @Override
    public int getColumnCount() throws SQLException
    {
        return columns.size();
    }

    @Override
    public boolean isAutoIncrement( int i ) throws SQLException
    {
        return false;
    }

    @Override
    public boolean isCaseSensitive( int i ) throws SQLException
    {
        return false;
    }

    @Override
    public boolean isSearchable( int i ) throws SQLException
    {
        return false;
    }

    @Override
    public boolean isCurrency( int i ) throws SQLException
    {
        return false;
    }

    @Override
    public int isNullable( int i ) throws SQLException
    {
        return 0;
    }

    @Override
    public boolean isSigned( int i ) throws SQLException
    {
        return false;
    }

    @Override
    public int getColumnDisplaySize( int i ) throws SQLException
    {
        return 20;
    }

    @Override
    public String getColumnLabel( int i ) throws SQLException
    {
        return columns.get( i - 1 ).getName();
    }

    @Override
    public String getColumnName( int i ) throws SQLException
    {
        return columns.get( i - 1 ).getName();
    }

    @Override
    public String getSchemaName( int i ) throws SQLException
    {
        return "Default";
    }

    @Override
    public int getPrecision( int i ) throws SQLException
    {
        return 0;
    }

    @Override
    public int getScale( int i ) throws SQLException
    {
        return 0;
    }

    @Override
    public String getTableName( int i ) throws SQLException
    {
        return null;
    }

    @Override
    public String getCatalogName( int i ) throws SQLException
    {
        return "Default";
    }

    @Override
    public int getColumnType( int i ) throws SQLException
    {
        return columns.get( i - 1 ).getDataType();
    }

    @Override
    public String getColumnTypeName( int i ) throws SQLException
    {
        return columns.get( i - 1 ).getTypeName();
    }

    @Override
    public boolean isReadOnly( int i ) throws SQLException
    {
        return false;
    }

    @Override
    public boolean isWritable( int i ) throws SQLException
    {
        return false;
    }

    @Override
    public boolean isDefinitelyWritable( int i ) throws SQLException
    {
        return false;
    }

    @Override
    public String getColumnClassName( int i ) throws SQLException
    {
        return columns.get( i - 1 ).getTypeName();
    }

    @Override
    public <T> T unwrap( Class<T> tClass ) throws SQLException
    {
        return (T) this;
    }

    @Override
    public boolean isWrapperFor( Class<?> aClass ) throws SQLException
    {
        return false;
    }
}
