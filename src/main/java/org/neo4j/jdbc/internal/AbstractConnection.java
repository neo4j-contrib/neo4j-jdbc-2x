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

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Struct;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * @author mh
 * @since 12.06.12
 */
public abstract class AbstractConnection implements Connection
{
    protected final Properties clientInfo;

    public AbstractConnection()
    {
        clientInfo = new Properties();
    }

    @Override
    public CallableStatement prepareCall( String sql ) throws SQLException
    {
        return null;
    }

    @Override
    public String nativeSQL( String sql ) throws SQLException
    {
        return sql;
    }

    @Override
    public void setCatalog( String catalog ) throws SQLException
    {
    }

    @Override
    public String getCatalog() throws SQLException
    {
        return "Default";
    }

    @Override
    public void setTransactionIsolation( int level ) throws SQLException
    {
    }

    @Override
    public int getTransactionIsolation() throws SQLException
    {
        return 0;
    }

    @Override
    public CallableStatement prepareCall( String sql, int resultSetType, int resultSetConcurrency ) throws SQLException
    {
        return null;
    }

    @Override
    public void setTypeMap( Map<String, Class<?>> map ) throws SQLException
    {
    }

    @Override
    public void setHoldability( int holdability ) throws SQLException
    {
    }

    @Override
    public Array createArrayOf( String typeName, Object[] elements ) throws SQLException
    {
        return null;
    }

    @Override
    public Struct createStruct( String typeName, Object[] attributes ) throws SQLException
    {
        return null;
    }

    @Override
    public <T> T unwrap( Class<T> iface ) throws SQLException
    {
        return (T) this;
    }

    @Override
    public boolean isWrapperFor( Class<?> iface ) throws SQLException
    {
        return false;
    }

    @Override
    public Clob createClob() throws SQLException
    {
        return null;
    }

    @Override
    public Blob createBlob() throws SQLException
    {
        return null;
    }

    @Override
    public NClob createNClob() throws SQLException
    {
        return null;
    }

    @Override
    public SQLXML createSQLXML() throws SQLException
    {
        return null;
    }

    @Override
    public CallableStatement prepareCall( String sql, int resultSetType, int resultSetConcurrency,
                                          int resultSetHoldability ) throws SQLException
    {
        return null;
    }

    @Override
    public int getHoldability() throws SQLException
    {
        return 0;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException
    {
        return null;
    }

    @Override
    public Savepoint setSavepoint( String name ) throws SQLException
    {
        return null;
    }

    @Override
    public void rollback( Savepoint savepoint ) throws SQLException
    {
    }

    @Override
    public void releaseSavepoint( Savepoint savepoint ) throws SQLException
    {
    }

    @Override
    public void setClientInfo( String name, String value ) throws SQLClientInfoException
    {
        clientInfo.setProperty( name, value );
    }

    @Override
    public void setClientInfo( Properties properties ) throws SQLClientInfoException
    {
        clientInfo.putAll( properties );
    }

    @Override
    public String getClientInfo( String name ) throws SQLException
    {
        return clientInfo.getProperty( name );
    }

    @Override
    public Properties getClientInfo() throws SQLException
    {
        return clientInfo;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException
    {
        return new HashMap<String, Class<?>>();
    }

    public void setSchema( String schema ) throws SQLException
    {

    }

    public String getSchema() throws SQLException
    {
        return null;
    }

    public void abort( Executor executor ) throws SQLException
    {
    }

    public void setNetworkTimeout( Executor executor, int milliseconds ) throws SQLException
    {
    }

    public int getNetworkTimeout() throws SQLException
    {
        return 0;
    }

}
