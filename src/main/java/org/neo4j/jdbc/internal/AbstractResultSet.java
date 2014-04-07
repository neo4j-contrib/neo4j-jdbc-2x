package org.neo4j.jdbc.internal;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * @author mh
 * @since 13.06.12
 */
public abstract class AbstractResultSet implements ResultSet
{
    protected final static Log log = LogFactory.getLog( AbstractResultSet.class );

    private boolean closed = false;
    private List<Neo4jColumnMetaData> columns;
    private Neo4jConnection conn;
    private String[] columnNames;
    private int cols;
    private boolean wasNull = false;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public boolean wasNull() throws SQLException
    {
        return wasNull;
    }

    @Override
    public String toString()
    {
        return "Columns: " + Arrays.toString( columnNames );
    }


    public AbstractResultSet( List<Neo4jColumnMetaData> columns, Neo4jConnection conn )
    {
        this.conn = conn;
        this.cols = columns.size();
        this.columns = columns;
        this.columnNames = extractColumnNames( columns );
    }

    public AbstractResultSet( Neo4jConnection conn, List<String> columns )
    {
        this.conn = conn;
        this.cols = columns.size();
        this.columnNames = columns.toArray( new String[cols] );
        this.columns = createMetadataFor( columns );
    }

    protected List<Neo4jColumnMetaData> createMetadataFor( List<String> columns )
    {
        List<Neo4jColumnMetaData> result = new ArrayList<Neo4jColumnMetaData>( columns.size() );
        for ( String column : columns )
        {
            result.add( new Neo4jColumnMetaData( column, "String", Types.VARCHAR ) );
        }
        return result;
    }

    private String[] extractColumnNames( List<Neo4jColumnMetaData> columns )
    {
        final String[] result = new String[columns.size()];
        Map<String, Integer> columnNames = new LinkedHashMap<String, Integer>();
        for ( int i = 0; i < cols; i++ )
        {
            result[i] = columns.get( i ).getName();
        }
        return result;
    }

    @Override
    public String getString( int i ) throws SQLException
    {
        Object value = get( i );

        if ( value == null )
        {
            return null;
        }
        final Class<?> type = value.getClass();
        if ( String.class.equals( type ) )
        {
            return (String) value;
        }
        if ( type.isPrimitive() || Number.class.isAssignableFrom( type ) )
        {
            return value.toString();
        }
        try
        {
            return OBJECT_MAPPER.writeValueAsString( value );
        }
        catch ( Exception e )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Couldn't convert value " + value + " of type " + type + " to JSON " + e.getMessage() );
            }
        }
        return value.toString();

    }

    @Override
    public boolean getBoolean( int i ) throws SQLException
    {
        return (Boolean) get( i );
    }

    @Override
    public byte getByte( int i ) throws SQLException
    {
        return ((Number) get( i )).byteValue();
    }

    @Override
    public short getShort( int i ) throws SQLException
    {
        return ((Number) get( i )).shortValue();
    }

    @Override
    public int getInt( int i ) throws SQLException
    {
        Object value = get( i );
        if ( value == null || !(value instanceof Integer) )
        {
            return 0;
        }
        else
        {
            return (Integer) value;
        }
    }

    @Override
    public long getLong( int i ) throws SQLException
    {
        return ((Number) get( i )).longValue();
    }

    private Object get( int column ) throws SQLDataException
    {
        if ( column < 1 || column > cols )
        {
            throw new SQLDataException( "Column " + column + " is invalid" );
        }
        Object value = currentRow()[column - 1];
        wasNull = value == null;
        return value;
    }

    protected abstract Object[] currentRow();

    @Override
    public float getFloat( int i ) throws SQLException
    {
        return ((Number) get( i )).floatValue();
    }

    @Override
    public double getDouble( int i ) throws SQLException
    {
        return ((Number) get( i )).doubleValue();
    }

    @Override
    public BigDecimal getBigDecimal( int i, int i1 ) throws SQLException
    {
        return (BigDecimal) get( i );
    }

    @Override
    public byte[] getBytes( int i ) throws SQLException
    {
        return (byte[]) get( i );
    }

    @Override
    public Date getDate( int i ) throws SQLException
    {
        return (Date) get( i );
    }

    @Override
    public Time getTime( int i ) throws SQLException
    {
        return (Time) get( i );
    }

    @Override
    public Timestamp getTimestamp( int i ) throws SQLException
    {
        return (Timestamp) get( i );
    }

    @Override
    public InputStream getAsciiStream( int i ) throws SQLException
    {
        return (InputStream) get( i );
    }

    @Override
    public InputStream getUnicodeStream( int i ) throws SQLException
    {
        return (InputStream) get( i );
    }

    @Override
    public InputStream getBinaryStream( int i ) throws SQLException
    {
        return (InputStream) get( i );
    }

    @Override
    public String getString( String s ) throws SQLException
    {
        return getString( findColumn( s ) );
    }

    @Override
    public boolean getBoolean( String s ) throws SQLException
    {
        return getBoolean( findColumn( s ) );
    }

    @Override
    public byte getByte( String s ) throws SQLException
    {
        return getByte( findColumn( s ) );
    }

    @Override
    public short getShort( String s ) throws SQLException
    {
        return getShort( findColumn( s ) );
    }

    @Override
    public int getInt( String s ) throws SQLException
    {
        return getInt( findColumn( s ) );
    }

    @Override
    public long getLong( String s ) throws SQLException
    {
        return getLong( findColumn( s ) );
    }

    @Override
    public float getFloat( String s ) throws SQLException
    {
        return getFloat( findColumn( s ) );
    }

    @Override
    public double getDouble( String s ) throws SQLException
    {
        return getDouble( findColumn( s ) );
    }

    @Override
    public BigDecimal getBigDecimal( String s, int i ) throws SQLException
    {
        return getBigDecimal( findColumn( s ) );
    }

    @Override
    public byte[] getBytes( String s ) throws SQLException
    {
        return getBytes( findColumn( s ) );
    }

    @Override
    public Date getDate( String s ) throws SQLException
    {
        return getDate( findColumn( s ) );
    }

    @Override
    public Time getTime( String s ) throws SQLException
    {
        return getTime( findColumn( s ) );
    }

    @Override
    public Timestamp getTimestamp( String s ) throws SQLException
    {
        return getTimestamp( findColumn( s ) );
    }

    @Override
    public InputStream getAsciiStream( String s ) throws SQLException
    {
        return getAsciiStream( findColumn( s ) );
    }

    @Override
    public InputStream getUnicodeStream( String s ) throws SQLException
    {
        return getUnicodeStream( findColumn( s ) );
    }

    @Override
    public InputStream getBinaryStream( String s ) throws SQLException
    {
        return getBinaryStream( findColumn( s ) );
    }

    @Override
    public SQLWarning getWarnings() throws SQLException
    {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException
    {
    }

    @Override
    public String getCursorName() throws SQLException
    {
        return null;
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException
    {
        return conn.debug( new Neo4jResultSetMetaData( columns ) );
    }

    @Override
    public Object getObject( int i ) throws SQLException
    {
        return get( i );
    }

    public <T> T getObject( int columnIndex, Class<T> type ) throws SQLException
    {
        return type.cast( getObject( columnIndex ) );
    }

    public <T> T getObject( String columnLabel, Class<T> type ) throws SQLException
    {
        return type.cast( getObject( columnLabel ) );
    }


    @Override
    public Object getObject( String s ) throws SQLException
    {
        return getObject( findColumn( s ) );
    }

    @Override
    public int findColumn( String column ) throws SQLException
    {
        if ( column != null )
        {
            for ( int i = 0; i < cols; i++ )
            {
                if ( column.equals( columnNames[i] ) )
                {
                    return i + 1;
                }
            }
        }
        throw new SQLException( "No such column:" + column );
    }

    @Override
    public Reader getCharacterStream( int i ) throws SQLException
    {
        return new StringReader( getString( i ) );
    }

    @Override
    public Reader getCharacterStream( String s ) throws SQLException
    {
        return new StringReader( getString( s ) );
    }

    @Override
    public BigDecimal getBigDecimal( int i ) throws SQLException
    {
        final double d = getDouble( i );
        final long l = getLong( i );
        return l == d ? BigDecimal.valueOf( l ) : BigDecimal.valueOf( d );
    }

    @Override
    public BigDecimal getBigDecimal( String s ) throws SQLException
    {
        return getBigDecimal( findColumn( s ) );
    }

    @Override
    public void setFetchDirection( int i ) throws SQLException
    {
    }

    @Override
    public int getFetchDirection() throws SQLException
    {
        return 0;
    }

    @Override
    public void setFetchSize( int i ) throws SQLException
    {
    }

    @Override
    public int getFetchSize() throws SQLException
    {
        return 0;
    }

    @Override
    public int getType() throws SQLException
    {
        return 0;
    }

    @Override
    public int getConcurrency() throws SQLException
    {
        return 0;
    }

    @Override
    public boolean rowUpdated() throws SQLException
    {
        return false;
    }

    @Override
    public boolean rowInserted() throws SQLException
    {
        return false;
    }

    @Override
    public boolean rowDeleted() throws SQLException
    {
        return false;
    }

    @Override
    public void updateNull( int i ) throws SQLException
    {
    }

    @Override
    public void updateBoolean( int i, boolean b ) throws SQLException
    {
    }

    @Override
    public void updateByte( int i, byte b ) throws SQLException
    {
    }

    @Override
    public void updateShort( int i, short i1 ) throws SQLException
    {
    }

    @Override
    public void updateInt( int i, int i1 ) throws SQLException
    {
    }

    @Override
    public void updateLong( int i, long l ) throws SQLException
    {
    }

    @Override
    public void updateFloat( int i, float v ) throws SQLException
    {
    }

    @Override
    public void updateDouble( int i, double v ) throws SQLException
    {
    }

    @Override
    public void updateBigDecimal( int i, BigDecimal bigDecimal ) throws SQLException
    {
    }

    @Override
    public void updateString( int i, String s ) throws SQLException
    {
    }

    @Override
    public void updateBytes( int i, byte[] bytes ) throws SQLException
    {
    }

    @Override
    public void updateDate( int i, Date date ) throws SQLException
    {
    }

    @Override
    public void updateTime( int i, Time time ) throws SQLException
    {
    }

    @Override
    public void updateTimestamp( int i, Timestamp timestamp ) throws SQLException
    {
    }

    @Override
    public void updateAsciiStream( int i, InputStream inputStream, int i1 ) throws SQLException
    {
    }

    @Override
    public void updateBinaryStream( int i, InputStream inputStream, int i1 ) throws SQLException
    {
    }

    @Override
    public void updateCharacterStream( int i, Reader reader, int i1 ) throws SQLException
    {
    }

    @Override
    public void updateObject( int i, Object o, int i1 ) throws SQLException
    {
    }

    @Override
    public void updateObject( int i, Object o ) throws SQLException
    {
    }

    @Override
    public void updateNull( String s ) throws SQLException
    {
    }

    @Override
    public void updateBoolean( String s, boolean b ) throws SQLException
    {
    }

    @Override
    public void updateByte( String s, byte b ) throws SQLException
    {
    }

    @Override
    public void updateShort( String s, short i ) throws SQLException
    {
    }

    @Override
    public void updateInt( String s, int i ) throws SQLException
    {
    }

    @Override
    public void updateLong( String s, long l ) throws SQLException
    {
    }

    @Override
    public void updateFloat( String s, float v ) throws SQLException
    {
    }

    @Override
    public void updateDouble( String s, double v ) throws SQLException
    {
    }

    @Override
    public void updateBigDecimal( String s, BigDecimal bigDecimal ) throws SQLException
    {
    }

    @Override
    public void updateString( String s, String s1 ) throws SQLException
    {
    }

    @Override
    public void updateBytes( String s, byte[] bytes ) throws SQLException
    {
    }

    @Override
    public void updateDate( String s, Date date ) throws SQLException
    {
    }

    @Override
    public void updateTime( String s, Time time ) throws SQLException
    {
    }

    @Override
    public void updateTimestamp( String s, Timestamp timestamp ) throws SQLException
    {
    }

    @Override
    public void updateAsciiStream( String s, InputStream inputStream, int i ) throws SQLException
    {
    }

    @Override
    public void updateBinaryStream( String s, InputStream inputStream, int i ) throws SQLException
    {
    }

    @Override
    public void updateCharacterStream( String s, Reader reader, int i ) throws SQLException
    {
    }

    @Override
    public void updateObject( String s, Object o, int i ) throws SQLException
    {
    }

    @Override
    public void updateObject( String s, Object o ) throws SQLException
    {
    }

    @Override
    public void insertRow() throws SQLException
    {
    }

    @Override
    public void updateRow() throws SQLException
    {
    }

    @Override
    public void deleteRow() throws SQLException
    {
    }

    @Override
    public void refreshRow() throws SQLException
    {
    }

    @Override
    public void cancelRowUpdates() throws SQLException
    {
    }

    @Override
    public void moveToInsertRow() throws SQLException
    {
    }

    @Override
    public void moveToCurrentRow() throws SQLException
    {
    }

    @Override
    public Statement getStatement() throws SQLException
    {
        return null; // todo
    }

    @Override
    public Object getObject( int i, Map<String, Class<?>> typeMap ) throws SQLException
    {
        return null;
    }

    @Override
    public Ref getRef( int i ) throws SQLException
    {
        return null;
    }

    @Override
    public Blob getBlob( int i ) throws SQLException
    {
        return null;
    }

    @Override
    public Clob getClob( int i ) throws SQLException
    {
        return null;
    }

    @Override
    public Array getArray( int i ) throws SQLException
    {
        return null;
    }

    @Override
    public Object getObject( String s, Map<String, Class<?>> typeMap ) throws SQLException
    {
        return getObject( findColumn( s ), typeMap );
    }

    @Override
    public Ref getRef( String s ) throws SQLException
    {
        return getRef( findColumn( s ) );
    }

    @Override
    public Blob getBlob( String s ) throws SQLException
    {
        return null;
    }

    @Override
    public Clob getClob( String s ) throws SQLException
    {
        return getClob( findColumn( s ) );
    }

    @Override
    public Array getArray( String s ) throws SQLException
    {
        return getArray( findColumn( s ) );
    }

    @Override
    public Date getDate( int i, Calendar calendar ) throws SQLException
    {
        return null;
    }

    @Override
    public Date getDate( String s, Calendar calendar ) throws SQLException
    {
        return getDate( findColumn( s ), calendar );
    }

    @Override
    public Time getTime( int i, Calendar calendar ) throws SQLException
    {
        return null;
    }

    @Override
    public Time getTime( String s, Calendar calendar ) throws SQLException
    {
        return getTime( findColumn( s ), calendar );
    }

    @Override
    public Timestamp getTimestamp( int i, Calendar calendar ) throws SQLException
    {
        return null;
    }

    @Override
    public Timestamp getTimestamp( String s, Calendar calendar ) throws SQLException
    {
        return getTimestamp( findColumn( s ), calendar );
    }

    @Override
    public URL getURL( int i ) throws SQLException
    {
        return null;
    }

    @Override
    public URL getURL( String s ) throws SQLException
    {
        return getURL( findColumn( s ) );
    }

    @Override
    public void updateRef( int i, Ref ref ) throws SQLException
    {
    }

    @Override
    public void updateRef( String s, Ref ref ) throws SQLException
    {
    }

    @Override
    public void updateBlob( int i, Blob blob ) throws SQLException
    {
    }

    @Override
    public void updateBlob( String s, Blob blob ) throws SQLException
    {
    }

    @Override
    public void updateClob( int i, Clob clob ) throws SQLException
    {
    }

    @Override
    public void updateClob( String s, Clob clob ) throws SQLException
    {
    }

    @Override
    public void updateArray( int i, Array array ) throws SQLException
    {
    }

    @Override
    public void updateArray( String s, Array array ) throws SQLException
    {
    }

    @Override
    public RowId getRowId( int i ) throws SQLException
    {
        return null;
    }

    @Override
    public RowId getRowId( String s ) throws SQLException
    {
        return null;
    }

    @Override
    public void updateRowId( int i, RowId rowId ) throws SQLException
    {
    }

    @Override
    public void updateRowId( String s, RowId rowId ) throws SQLException
    {
    }

    @Override
    public int getHoldability() throws SQLException
    {
        return 0;
    }

    @Override
    public void updateNString( int i, String s ) throws SQLException
    {
    }

    @Override
    public void updateNString( String s, String s1 ) throws SQLException
    {
    }

    @Override
    public void updateNClob( int i, NClob nClob ) throws SQLException
    {
    }

    @Override
    public void updateNClob( String s, NClob nClob ) throws SQLException
    {
    }

    @Override
    public NClob getNClob( int i ) throws SQLException
    {
        return null;
    }

    @Override
    public NClob getNClob( String s ) throws SQLException
    {
        return null;
    }

    @Override
    public SQLXML getSQLXML( int i ) throws SQLException
    {
        return null;
    }

    @Override
    public SQLXML getSQLXML( String s ) throws SQLException
    {
        return null;
    }

    @Override
    public void updateSQLXML( int i, SQLXML sqlxml ) throws SQLException
    {
    }

    @Override
    public void updateSQLXML( String s, SQLXML sqlxml ) throws SQLException
    {
    }

    @Override
    public String getNString( int i ) throws SQLException
    {
        return null;
    }

    @Override
    public String getNString( String s ) throws SQLException
    {
        return null;
    }

    @Override
    public Reader getNCharacterStream( int i ) throws SQLException
    {
        return null;
    }

    @Override
    public Reader getNCharacterStream( String s ) throws SQLException
    {
        return null;
    }

    @Override
    public void updateNCharacterStream( int i, Reader reader, long l ) throws SQLException
    {
    }

    @Override
    public void updateNCharacterStream( String s, Reader reader, long l ) throws SQLException
    {
    }

    @Override
    public void updateAsciiStream( int i, InputStream inputStream, long l ) throws SQLException
    {
    }

    @Override
    public void updateBinaryStream( int i, InputStream inputStream, long l ) throws SQLException
    {
    }

    @Override
    public void updateCharacterStream( int i, Reader reader, long l ) throws SQLException
    {
    }

    @Override
    public void updateAsciiStream( String s, InputStream inputStream, long l ) throws SQLException
    {
    }

    @Override
    public void updateBinaryStream( String s, InputStream inputStream, long l ) throws SQLException
    {
    }

    @Override
    public void updateCharacterStream( String s, Reader reader, long l ) throws SQLException
    {
    }

    @Override
    public void updateBlob( int i, InputStream inputStream, long l ) throws SQLException
    {
    }

    @Override
    public void updateBlob( String s, InputStream inputStream, long l ) throws SQLException
    {
    }

    @Override
    public void updateClob( int i, Reader reader, long l ) throws SQLException
    {
    }

    @Override
    public void updateClob( String s, Reader reader, long l ) throws SQLException
    {
    }

    @Override
    public void updateNClob( int i, Reader reader, long l ) throws SQLException
    {
    }

    @Override
    public void updateNClob( String s, Reader reader, long l ) throws SQLException
    {
    }

    @Override
    public void updateNCharacterStream( int i, Reader reader ) throws SQLException
    {
    }

    @Override
    public void updateNCharacterStream( String s, Reader reader ) throws SQLException
    {
    }

    @Override
    public void updateAsciiStream( int i, InputStream inputStream ) throws SQLException
    {
    }

    @Override
    public void updateBinaryStream( int i, InputStream inputStream ) throws SQLException
    {
    }

    @Override
    public void updateCharacterStream( int i, Reader reader ) throws SQLException
    {
    }

    @Override
    public void updateAsciiStream( String s, InputStream inputStream ) throws SQLException
    {
    }

    @Override
    public void updateBinaryStream( String s, InputStream inputStream ) throws SQLException
    {
    }

    @Override
    public void updateCharacterStream( String s, Reader reader ) throws SQLException
    {
    }

    @Override
    public void updateBlob( int i, InputStream inputStream ) throws SQLException
    {
    }

    @Override
    public void updateBlob( String s, InputStream inputStream ) throws SQLException
    {
    }

    @Override
    public void updateClob( int i, Reader reader ) throws SQLException
    {
    }

    @Override
    public void updateClob( String s, Reader reader ) throws SQLException
    {
    }

    @Override
    public void updateNClob( int i, Reader reader ) throws SQLException
    {
    }

    @Override
    public void updateNClob( String s, Reader reader ) throws SQLException
    {
    }

    @Override
    public <T> T unwrap( Class<T> tClass ) throws SQLException
    {
        return null;
    }

    @Override
    public boolean isWrapperFor( Class<?> aClass ) throws SQLException
    {
        return false;
    }

    @Override
    public void close() throws SQLException
    {
        closed = true;
    }

    @Override
    public boolean isClosed() throws SQLException
    {
        return closed;
    }
}
