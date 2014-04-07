package org.neo4j.jdbc.internal.rest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.codehaus.jackson.JsonNode;

/**
 * @author mh
 * @since 12.06.12
 */
// TODO Implement support for streaming Cypher here implements Iterator<Map<String,Object>>
public class ResultParser
{
    private final JsonNode node;
    private List<String> columns;
    private int cols;
    private final Object[] rowData;

    public ResultParser( JsonNode node )
    {
        this.node = node;
        this.columns = parseColumns();
        this.cols = columns.size();
        rowData = new Object[cols];
    }

    public List<String> getColumns()
    {
        return columns;
    }

    public List<String> parseColumns()
    {
        List<String> columns = new ArrayList<String>( 20 );
        for ( JsonNode column : node.get( "columns" ) )
        {
            String textValue = column.getTextValue();
            columns.add( textValue );
        }
        return columns;
    }

    Iterator<Object[]> streamData()
    {
        final Iterator<JsonNode> rows = node.get( "data" ).iterator();
        return new Iterator<Object[]>()
        {
            public boolean hasNext()
            {
                return rows.hasNext();
            }

            public Object[] next()
            {
                return parseRow( rows.next() );
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    private Object[] parseRow( JsonNode row )
    {
        int i = 0;
        for ( JsonNode cell : row )
        {
            rowData[i++] = toObject( cell );
        }
        for (; i < cols; i++ )
        {
            rowData[i] = null;
        }
        return rowData;
    }

    private Object toObject( JsonNode cell )
    {
        if ( cell.isObject() )
        {
            if ( cell.has( "length" ) && cell.has( "nodes" ) && cell.has( "relationships" ) )
            {
                return toPath( cell );
            }
            Map<String, Object> result = toPropertyContainer( cell );
            return addRelationshipInfo( cell, result );
        }
        if ( cell.isArray() )
        {
            ArrayList<Object> result = new ArrayList<Object>( cell.size() );
            for ( JsonNode node : cell )
            {
                result.add( toObject( node ) );
            }
            return result;
        }
        if ( cell.isTextual() )
        {
            return cell.getTextValue();
        }
        if ( cell.isBoolean() )
        {
            return cell.getBooleanValue();
        }
        if ( cell.isNumber() )
        {
            return cell.getNumberValue();
        }
        return cell.getTextValue();
    }

    private Object addRelationshipInfo( JsonNode cell, Map<String, Object> result )
    {
        if ( cell.has( "start" ) )
        {
            result.put( "_start", idOf( cell.get( "start" ) ) );
        }
        if ( cell.has( "end" ) )
        {
            result.put( "_end", idOf( cell.get( "end" ) ) );
        }
        if ( cell.has( "type" ) )
        {
            result.put( "_type", cell.get( "type" ).getTextValue() );
        }
        return result;
    }

    private Map<String, Object> toPropertyContainer( JsonNode cell )
    {
        Map<String, Object> result = new TreeMap<String, Object>();
        String idField = cell.has( "type" ) ? "_rel_id" : "_node_id";
        result.put( idField, idOf( cell.get( "self" ) ) );
        JsonNode data = cell.get( "data" );
        if ( data != null && data.isObject() && data.size() > 0 )
        {
            Iterator<String> fieldNames = data.getFieldNames();
            while ( fieldNames.hasNext() )
            {
                String fieldName = fieldNames.next();
                result.put( fieldName, toObject( data.get( fieldName ) ) );
            }
        }
        return result;
    }

    private ArrayList<Object> toPath( JsonNode cell )
    {
        ArrayList<Object> path = new ArrayList<Object>( cell.get( "length" ).getIntValue() + 1 );
        Iterator<JsonNode> nodes = cell.get( "nodes" ).iterator();
        Iterator<JsonNode> relationships = cell.get( "relationships" ).iterator();
        while ( nodes.hasNext() )
        {
            path.add( map( "_node_id", idOf( nodes.next() ) ) );
            if ( relationships.hasNext() )
            {
                path.add( map( "_rel_id", idOf( relationships.next() ) ) );
            }
        }
        return path;
    }

    private Map<String, Object> map( String key, Object value )
    {
        TreeMap<String, Object> result = new TreeMap<String, Object>();
        result.put( key, value );
        return result;
    }

    private Long idOf( JsonNode node )
    {
        if ( node == null )
        {
            return null;
        }
        String uri = node.getTextValue();
        if ( uri == null )
        {
            return null;
        }
        int idx = uri.lastIndexOf( "/" );
        return idx == -1 ? null : Long.valueOf( uri.substring( idx + 1 ) );
    }
}
