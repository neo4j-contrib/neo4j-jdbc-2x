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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of PreparedStatement. Parameters in Cypher queries have to be done as {nr}, as calls to methods
 * here will be saved in a parameter map with "nr"->value, since JDBC does not support named parameters.
 */
public class Neo4jPreparedStatement extends AbstractPreparedStatement
{
    private String query;
    private Map<String, Object> parameters = new HashMap<String, Object>();

    public Neo4jPreparedStatement( Neo4jConnection connection, String query )
    {
        super( connection );
        this.query = query;
    }

    @Override
    public ResultSet executeQuery() throws SQLException
    {
        resultSet = connection.executeQuery( query, parameters );
        return resultSet;
    }

    @Override
    public boolean execute() throws SQLException
    {
        resultSet = connection.executeQuery( query, parameters );
        return true;
    }

    @Override
    public int executeUpdate() throws SQLException
    {
        resultSet = connection.executeQuery( query, parameters );
        while ( resultSet.next() )
        {
            ;
        }
        return 0; // todo
    }

    private void add( int parameterIndex, Object value )
    {
        parameters.put( Integer.toString( parameterIndex ), value );
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException
    {
        if ( resultSet == null )
        {
            execute();
        }

        return resultSet.getMetaData();
    }

    @Override
    public void setNull( int parameterIndex, int sqlType ) throws SQLException
    {
        add( parameterIndex, null );
        // todo is that the correct behaviour for cypher ?
        // parameters.remove(Integer.toString(parameterIndex));
    }

    @Override
    public void setBoolean( int parameterIndex, boolean value ) throws SQLException
    {
        add( parameterIndex, value );
    }

    @Override
    public void setByte( int parameterIndex, byte value ) throws SQLException
    {
        add( parameterIndex, value );
    }

    @Override
    public void setShort( int parameterIndex, short value ) throws SQLException
    {
        add( parameterIndex, value );
    }

    @Override
    public void setInt( int parameterIndex, int value ) throws SQLException
    {
        add( parameterIndex, value );
    }

    @Override
    public void setLong( int parameterIndex, long value ) throws SQLException
    {
        add( parameterIndex, value );
    }

    @Override
    public void setFloat( int parameterIndex, float value ) throws SQLException
    {
        add( parameterIndex, value );
    }

    @Override
    public void setDouble( int parameterIndex, double value ) throws SQLException
    {
        add( parameterIndex, value );
    }

    @Override
    public void setBigDecimal( int parameterIndex, BigDecimal value ) throws SQLException
    {
        add( parameterIndex, value );
    }

    @Override
    public void setString( int parameterIndex, String value ) throws SQLException
    {
        add( parameterIndex, value );
    }

    @Override
    public void setBytes( int parameterIndex, byte[] value ) throws SQLException
    {
        add( parameterIndex, value );
    }

    @Override
    public void clearParameters() throws SQLException
    {
        parameters.clear();
    }

    @Override
    public void setObject( int parameterIndex, Object value, int targetSqlType ) throws SQLException
    {
        add( parameterIndex, value );
    }

    @Override
    public void setObject( int parameterIndex, Object value ) throws SQLException
    {
        add( parameterIndex, value );
    }

    @Override
    public void setNull( int parameterIndex, int sqlType, String typeName ) throws SQLException
    {
        add( parameterIndex, null );
        // parameters.remove(Integer.toString(parameterIndex));
    }

    @Override
    public void setObject( int parameterIndex, Object value, int targetSqlType, int scaleOrLength ) throws SQLException
    {
        add( parameterIndex, value );
    }
}
