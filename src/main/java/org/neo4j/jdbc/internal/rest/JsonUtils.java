package org.neo4j.jdbc.internal.rest;

import java.math.BigDecimal;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

/**
 * @author mh
 * @since 10.02.14
 */
public class JsonUtils
{

    static ObjectNode serialize( Map<String, Object> parameters, ObjectMapper mapper )
    {
        ObjectNode params = mapper.createObjectNode();
        for ( Map.Entry<String, Object> entry : parameters.entrySet() )
        {
            final String name = entry.getKey();
            final Object value = entry.getValue();
            if ( value == null )
            {
                params.putNull( name );
            }
            else if ( value instanceof String )
            {
                params.put( name, value.toString() );
            }
            else if ( value instanceof Integer )
            {
                params.put( name, (Integer) value );
            }
            else if ( value instanceof Long )
            {
                params.put( name, (Long) value );
            }
            else if ( value instanceof Boolean )
            {
                params.put( name, (Boolean) value );
            }
            else if ( value instanceof BigDecimal )
            {
                params.put( name, (BigDecimal) value );
            }
            else if ( value instanceof Double )
            {
                params.put( name, (Double) value );
            }
            else if ( value instanceof byte[] )
            {
                params.put( name, (byte[]) value );
            }
            else if ( value instanceof Float )
            {
                params.put( name, (Float) value );
            }
            else if ( value instanceof Number )
            {
                final Number number = (Number) value;
                if ( number.longValue() == number.doubleValue() )
                {
                    params.put( name, number.longValue() );
                }
                else
                {
                    params.put( name, number.doubleValue() );
                }
            }
            else if ( value instanceof Map )
            {
                params.put( name, serialize( (Map<String, Object>) value, mapper ) );
            }
            else if ( value instanceof Iterable )
            {
                params.put( name, serialize( (Iterable) value, mapper ) );
            }
            else
            {
                throw new IllegalArgumentException( "Could not serialize value " + entry.getKey() + " " + entry
                        .getValue() );
            }
        }
        return params;
    }

    static ArrayNode serialize( Iterable<Object> iterable, ObjectMapper mapper )
    {
        ArrayNode array = mapper.createArrayNode();
        for ( Object value : iterable )
        {
            if ( value == null )
            {
                array.addNull();
            }
            else if ( value instanceof String )
            {
                array.add( value.toString() );
            }
            else if ( value instanceof Integer )
            {
                array.add( (Integer) value );
            }
            else if ( value instanceof Long )
            {
                array.add( (Long) value );
            }
            else if ( value instanceof Boolean )
            {
                array.add( (Boolean) value );
            }
            else if ( value instanceof BigDecimal )
            {
                array.add( (BigDecimal) value );
            }
            else if ( value instanceof Double )
            {
                array.add( (Double) value );
            }
            else if ( value instanceof byte[] )
            {
                array.add( (byte[]) value );
            }
            else if ( value instanceof Float )
            {
                array.add( (Float) value );
            }
            else if ( value instanceof Number )
            {
                final Number number = (Number) value;
                if ( number.longValue() == number.doubleValue() )
                {
                    array.add( number.longValue() );
                }
                else
                {
                    array.add( number.doubleValue() );
                }
            }
            else if ( value instanceof Map )
            {
                array.add( serialize( (Map<String, Object>) value, mapper ) );
            }
            else if ( value instanceof Iterable )
            {
                array.add( serialize( (Iterable) value, mapper ) );
            }
            else
            {
                throw new IllegalArgumentException( "Could not serialize value " + value );
            }
        }
        return array;
    }
}
