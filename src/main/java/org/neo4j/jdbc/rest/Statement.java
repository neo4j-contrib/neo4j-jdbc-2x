package org.neo4j.jdbc.rest;

import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

/**
 * @author mh
 * @since 20.04.13
 */
public class Statement
{
    final String query;
    final Map<String, Object> params;

    Statement( String query, Map<String, Object> params )
    {
        this.query = query;
        this.params = params;
    }

    public ObjectNode toJson( ObjectMapper mapper )
    {
        ObjectNode queryNode = mapper.createObjectNode();
        queryNode.put( "statement", JsonUtils.escapeQuery( query ) );
        if ( params != null && !params.isEmpty() )
        {
            queryNode.put( "parameters", JsonUtils.serialize( params, mapper ) );
        }
        return queryNode;
    }

    @Override
    public String toString()
    {
        return "query: " + query + "\nparams:" + params;
    }

    public static ArrayNode toJson( ObjectMapper mapper, Statement... statements )
    {
        ArrayNode result = mapper.createArrayNode();
        if ( statements == null || statements.length == 0 )
        {
            return result;
        }
        for ( Statement statement : statements )
        {
            result.add( statement.toJson( mapper ) );
        }
        return result;
    }
}
