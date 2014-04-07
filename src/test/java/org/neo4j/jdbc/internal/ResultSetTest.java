package org.neo4j.jdbc.internal;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

/**
 * @author mh
 * @since 13.06.12
 */
@RunWith(Parameterized.class)
public class ResultSetTest
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final ResultSet rs;
    private static List<Object> row;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static Collection<ResultSet[]> data()
    {
        row = Arrays.<Object>asList( "0", 1, (short) 2, 3L, (byte) 4, 5f, 6d, BigDecimal.valueOf( 7L ), null,
                new String[]{"array"}, Collections.singletonMap( "a", 1 ), Collections.singletonList( "list" ) );

        List<Neo4jColumnMetaData> columns = asList(
                col( "String", Types.VARCHAR ),
                col( "int", Types.INTEGER ),
                col( "short", Types.SMALLINT ),
                col( "long", Types.BIGINT ),
                col( "byte", Types.TINYINT ),
                col( "float", Types.FLOAT ),
                col( "double", Types.DOUBLE ),
                col( "BigDecimal", Types.NUMERIC ),
                col( "Null", Types.NULL ),
                col( "Array", Types.ARRAY ),
                col( "Map", Types.STRUCT ),
                col( "List", Types.ARRAY )
        );

        return Arrays.<ResultSet[]>asList( new ResultSet[]{new ListResultSet( columns,
                Arrays.<List<Object>>asList( row ), null )},
                new ResultSet[]{new IteratorResultSet( columns, Arrays.<Object[]>asList( row.toArray() ).iterator(),
                        null )}
        );
    }

    public ResultSetTest( ResultSet rs )
    {
        this.rs = rs;
    }

    @Test
    public void testGetByIndex() throws Exception
    {
        assertTrue( rs.next() );
        assertEquals( row.get( 0 ), rs.getString( 1 ) );
        assertEquals( row.get( 0 ), rs.getString( "String" ) );

        assertEquals( row.get( 1 ), rs.getInt( 2 ) );
        assertEquals( row.get( 1 ), rs.getInt( "int" ) );
        assertEquals( row.get( 2 ), rs.getShort( 3 ) );
        assertEquals( row.get( 2 ), rs.getShort( "short" ) );
        assertEquals( row.get( 3 ), rs.getLong( 4 ) );
        assertEquals( row.get( 3 ), rs.getLong( "long" ) );
        assertEquals( row.get( 4 ), rs.getByte( 5 ) );
        assertEquals( row.get( 4 ), rs.getByte( "byte" ) );
        assertEquals( row.get( 5 ), rs.getFloat( 6 ) );
        assertEquals( row.get( 5 ), rs.getFloat( "float" ) );
        assertEquals( row.get( 6 ), rs.getDouble( 7 ) );
        assertEquals( row.get( 6 ), rs.getDouble( "double" ) );
        assertEquals( row.get( 7 ), rs.getBigDecimal( 8 ) );
        assertEquals( row.get( 7 ), rs.getBigDecimal( "BigDecimal" ) );
        assertEquals( false, rs.wasNull() );
        assertEquals( row.get( 8 ), rs.getObject( "Null" ) );
        assertEquals( true, rs.wasNull() );
        assertEquals( row.get( 8 ), rs.getObject( 9 ) );
        assertEquals( true, rs.wasNull() );
        assertEquals( row.get( 9 ), rs.getObject( "Array" ) );
        assertEquals( row.get( 9 ), rs.getObject( 10 ) );
        assertEquals( OBJECT_MAPPER.writeValueAsString( row.get( 9 ) ), rs.getString( "Array" ) );
        assertEquals( OBJECT_MAPPER.writeValueAsString( row.get( 9 ) ), rs.getString( 10 ) );

        assertEquals( row.get( 10 ), rs.getObject( "Map" ) );
        assertEquals( row.get( 10 ), rs.getObject( 11 ) );
        assertEquals( OBJECT_MAPPER.writeValueAsString( row.get( 10 ) ), rs.getString( "Map" ) );
        assertEquals( OBJECT_MAPPER.writeValueAsString( row.get( 10 ) ), rs.getString( 11 ) );

        assertEquals( row.get( 11 ), rs.getObject( "List" ) );
        assertEquals( row.get( 11 ), rs.getObject( 12 ) );
        assertEquals( OBJECT_MAPPER.writeValueAsString( row.get( 11 ) ), rs.getString( "List" ) );
        assertEquals( OBJECT_MAPPER.writeValueAsString( row.get( 11 ) ), rs.getString( 12 ) );

        assertFalse( rs.next() );
    }

    private static Neo4jColumnMetaData col( String typeName, int type )
    {
        return new Neo4jColumnMetaData( typeName, typeName, type );
    }
}
