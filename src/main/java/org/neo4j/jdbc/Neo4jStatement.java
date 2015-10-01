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
package org.neo4j.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Collections;

/**
 * Implementation of JDBC Statement.
 */
public class Neo4jStatement
        implements Statement
{
    protected Neo4jConnection connection;
    protected ResultSet resultSet;
    protected SQLWarning sqlWarning;

    public Neo4jStatement( Neo4jConnection connection )
    {
        this.connection = connection;
    }

    @Override
    public ResultSet executeQuery( String s ) throws SQLException
    {
        execute( s );
        return resultSet;
    }

    @Override
    public int executeUpdate( String s ) throws SQLException
    {
        execute( s );
        // TODO return actual update count
        return 0;
    }

    @Override
    public void close() throws SQLException
    {
        if ( resultSet != null )
        {
            resultSet.close();
        }
        connection = null;
        resultSet = null;
        sqlWarning = null;
    }

    @Override
    public int getMaxFieldSize() throws SQLException
    {
        throw unsupported( "getMaxFieldSize" );
    }

    @Override
    public void setMaxFieldSize( int i ) throws SQLException
    {
        throw unsupported( "setMaxFieldSize " );
    }

    @Override
    public int getMaxRows() throws SQLException
    {
        throw unsupported( "getMaxRows" );
    }

    @Override
    public void setMaxRows( int i ) throws SQLException
    {
        throw unsupported( "setMaxRows" );
    }

    @Override
    public void setEscapeProcessing( boolean b ) throws SQLException
    {
        throw unsupported( "setEscapeProcessing" );
    }

    @Override
    public int getQueryTimeout() throws SQLException
    {
        throw unsupported( "getQueryTimeout" );
    }

    @Override
    public void setQueryTimeout( int i ) throws SQLException
    {
        throw unsupported( "setQueryTimeout" );
    }

    @Override
    public void cancel() throws SQLException
    {
        throw unsupported( "cancel" );
    }

    @Override
    public SQLWarning getWarnings() throws SQLException
    {
        return sqlWarning;
    }

    @Override
    public void clearWarnings() throws SQLException
    {
        sqlWarning = null;
    }

    @Override
    public void setCursorName( String s ) throws SQLException
    {
        throw unsupported( "setCursorName" );
    }

    @Override
    public boolean execute( String s ) throws SQLException
    {
        try
        {
            resultSet = connection.executeQuery( connection.nativeSQL( s ), Collections.<String, Object>emptyMap() );
            return true;
        }
        catch ( SQLWarning e )
        {
            if ( sqlWarning == null )
            {
                sqlWarning = e;
            }
            else
            {
                sqlWarning.setNextWarning( e );
            }
            throw e;
        }
        catch ( SQLException e )
        {
            throw e;
        }
        catch ( Throwable e )
        {
            throw new SQLException( e );
        }
    }

    @Override
    public ResultSet getResultSet() throws SQLException
    {
        return resultSet;
    }

    @Override
    public int getUpdateCount() throws SQLException
    {
        // TODO return actual update count
        // throw unsupported( "getUpdateCount" );
        return -1;
    }

    @Override
    public boolean getMoreResults() throws SQLException
    {
        resultSet = null;
        return false;
    }

    @Override
    public void setFetchDirection( int i ) throws SQLException
    {
        throw unsupported( "setFetchDirection" );
    }

    @Override
    public int getFetchDirection() throws SQLException
    {
        return ResultSet.FETCH_UNKNOWN;
    }

    @Override
    public void setFetchSize( int i ) throws SQLException
    {
        throw unsupported( "setFetchSize" );
    }

    @Override
    public int getFetchSize() throws SQLException
    {
        throw unsupported( "getFetchSize" );
    }

    @Override
    public int getResultSetConcurrency() throws SQLException
    {
        return ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public int getResultSetType() throws SQLException
    {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public void addBatch( String s ) throws SQLException
    {
        throw unsupported( "addBatch" );
    }

    @Override
    public void clearBatch() throws SQLException
    {
        throw unsupported( "clearBatch" );
    }

    @Override
    public int[] executeBatch() throws SQLException
    {
        throw unsupported( "executeBatch" );
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        return connection;
    }

    @Override
    public boolean getMoreResults( int i ) throws SQLException
    {
        return getMoreResults();
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException
    {
        return new ResultSetBuilder().newResultSet( connection );
    }

    @Override
    public int executeUpdate( String s, int i ) throws SQLException
    {
        return executeUpdate( s );
    }

    @Override
    public int executeUpdate( String s, int[] ints ) throws SQLException
    {
        return executeUpdate( s );
    }

    @Override
    public int executeUpdate( String s, String[] strings ) throws SQLException
    {
        return executeUpdate( s );
    }

    @Override
    public boolean execute( String s, int i ) throws SQLException
    {
        return execute( s );
    }

    @Override
    public boolean execute( String s, int[] ints ) throws SQLException
    {
        return execute( s );
    }

    @Override
    public boolean execute( String s, String[] strings ) throws SQLException
    {
        return execute( s );
    }

    @Override
    public int getResultSetHoldability() throws SQLException
    {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    @Override
    public boolean isClosed() throws SQLException
    {
        return connection == null;
    }

    @Override
    public void setPoolable( boolean b ) throws SQLException
    {
        throw unsupported( "setPoolable" );
    }

    @Override
    public boolean isPoolable() throws SQLException
    {
        return false;
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

    public void closeOnCompletion() throws SQLException
    {
        throw unsupported( "closeOnCompletion" );
    }

    public boolean isCloseOnCompletion() throws SQLException
    {
        return false;
    }

    private static SQLFeatureNotSupportedException unsupported( String methodName )
    {
        return new SQLFeatureNotSupportedException( methodName + " is not supported by Neo4jStatement." );
    }
}
