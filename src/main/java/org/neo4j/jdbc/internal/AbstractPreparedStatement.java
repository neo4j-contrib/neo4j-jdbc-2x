package org.neo4j.jdbc.internal;

import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * @author mh
 * @since 12.06.12
 */
public abstract class AbstractPreparedStatement extends Neo4jStatement implements PreparedStatement
{
    public AbstractPreparedStatement( Neo4jConnection connection )
    {
        super( connection );
    }


    @Override
    public void setAsciiStream( int parameterIndex, InputStream value, long length ) throws SQLException
    {
        throw new SQLException( "Streams not supported as parameters" );
    }

    @Override
    public void setBinaryStream( int parameterIndex, InputStream value, long length ) throws SQLException
    {
        throw new SQLException( "Streams not supported as parameters" );
    }

    @Override
    public void setCharacterStream( int parameterIndex, Reader reader, long length ) throws SQLException
    {
        throw new SQLException( "Streams not supported as parameters" );
    }

    @Override
    public void setAsciiStream( int parameterIndex, InputStream x ) throws SQLException
    {
        throw new SQLException( "Streams not supported as parameters" );
    }

    @Override
    public void setBinaryStream( int parameterIndex, InputStream x ) throws SQLException
    {
        throw new SQLException( "Streams not supported as parameters" );
    }

    @Override
    public void setCharacterStream( int parameterIndex, Reader reader ) throws SQLException
    {
        throw new SQLException( "Streams not supported as parameters" );
    }

    @Override
    public void setNCharacterStream( int parameterIndex, Reader value ) throws SQLException
    {
        throw new SQLException( "Streams not supported as parameters" );
    }

    @Override
    public void setClob( int parameterIndex, Reader reader ) throws SQLException
    {
        throw new SQLException( "Clobs not supported as parameters" );
    }

    @Override
    public void setBlob( int parameterIndex, InputStream inputStream ) throws SQLException
    {
        throw new SQLException( "Blobs not supported as parameters" );
    }

    @Override
    public void setNClob( int parameterIndex, Reader reader ) throws SQLException
    {
        throw new SQLException( "Clobs not supported as parameters" );
    }

    @Override
    public void setDate( int parameterIndex, Date x ) throws SQLException
    {
        throw new SQLException( "Dates not supported as parameters" );
    }

    @Override
    public void setTime( int parameterIndex, Time x ) throws SQLException
    {
        throw new SQLException( "Times not supported as parameters" );
    }

    @Override
    public void setTimestamp( int parameterIndex, Timestamp x ) throws SQLException
    {
        throw new SQLException( "Timestamps not supported as parameters" );
    }

    @Override
    public void setAsciiStream( int parameterIndex, InputStream x, int length ) throws SQLException
    {
        throw new SQLException( "Streams not supported as parameters" );
    }

    @Override
    public void setUnicodeStream( int parameterIndex, InputStream x, int length ) throws SQLException
    {
        throw new SQLException( "Streams not supported as parameters" );
    }

    @Override
    public void setBinaryStream( int parameterIndex, InputStream x, int length ) throws SQLException
    {
        throw new SQLException( "Streams not supported as parameters" );
    }

    @Override
    public void addBatch() throws SQLException
    {
        throw new SQLException( "Batch operations not supported" );
    }

    @Override
    public void setCharacterStream( int parameterIndex, Reader reader, int length ) throws SQLException
    {
        throw new SQLException( "Streams not supported as parameters" );
    }

    @Override
    public void setRef( int parameterIndex, Ref x ) throws SQLException
    {
        throw new SQLException( "Refs not supported as parameters" );
    }

    @Override
    public void setBlob( int parameterIndex, Blob x ) throws SQLException
    {
        throw new SQLException( "Blobs not supported as parameters" );
    }

    @Override
    public void setClob( int parameterIndex, Clob x ) throws SQLException
    {
        throw new SQLException( "Clobs not supported as parameters" );
    }

    @Override
    public void setArray( int parameterIndex, Array x ) throws SQLException
    {
        throw new SQLException( "Arrays not supported as parameters" );
    }

    @Override
    public void setDate( int parameterIndex, Date x, Calendar cal ) throws SQLException
    {
        throw new SQLException( "Dates not supported as parameters" );
    }

    @Override
    public void setTime( int parameterIndex, Time x, Calendar cal ) throws SQLException
    {
        throw new SQLException( "Times not supported as parameters" );
    }

    @Override
    public void setTimestamp( int parameterIndex, Timestamp x, Calendar cal ) throws SQLException
    {
        throw new SQLException( "Timestamps not supported as parameters" );
    }

    @Override
    public void setURL( int parameterIndex, URL x ) throws SQLException
    {
        throw new SQLException( "Streams not supported as parameters" );
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException
    {
        return null;
    }

    @Override
    public void setRowId( int parameterIndex, RowId x ) throws SQLException
    {
        throw new SQLException( "RowId not supported as parameters" );
    }

    @Override
    public void setNString( int parameterIndex, String value ) throws SQLException
    {
        throw new SQLException( "NString not supported as parameters" );
    }

    @Override
    public void setNCharacterStream( int parameterIndex, Reader value, long length ) throws SQLException
    {
        throw new SQLException( "Streams not supported as parameters" );
    }

    @Override
    public void setNClob( int parameterIndex, NClob value ) throws SQLException
    {
        throw new SQLException( "Clobs not supported as parameters" );
    }

    @Override
    public void setClob( int parameterIndex, Reader reader, long length ) throws SQLException
    {
        throw new SQLException( "Clobs not supported as parameters" );
    }

    @Override
    public void setBlob( int parameterIndex, InputStream inputStream, long length ) throws SQLException
    {
        throw new SQLException( "Blobs not supported as parameters" );
    }

    @Override
    public void setNClob( int parameterIndex, Reader reader, long length ) throws SQLException
    {
        throw new SQLException( "Clobs not supported as parameters" );
    }

    @Override
    public void setSQLXML( int parameterIndex, SQLXML xmlObject ) throws SQLException
    {
        throw new SQLException( "SQLXML not supported as parameters" );
    }
}
