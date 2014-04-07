package org.neo4j.jdbc.internal;

import java.util.LinkedHashMap;
import java.util.Map;

import org.neo4j.cypherdsl.grammar.Execute;
import org.neo4j.cypherdsl.grammar.ExecuteWithParameters;
import org.neo4j.cypherdsl.query.Query;

/**
 * @author mh
 * @since 20.12.13
 */
public class QueryStringBasedExecute implements Execute, ExecuteWithParameters
{

    private final String query;
    private final Map<String, Object> params = new LinkedHashMap<>();

    @Override
    public Query toQuery()
    {
        throw new UnsupportedOperationException();
    }

    public QueryStringBasedExecute( String query )
    {
        this.query = query;
    }

    @Override
    public ExecuteWithParameters parameter( String name, Object value )
    {
        params.put( name, value );
        return this;
    }

    @Override
    public ExecuteWithParameters parameters( Map<String, Object> parameters )
    {
        params.putAll( parameters );
        return this;
    }

    @Override
    public void asString( StringBuilder builder )
    {
        builder.append( query );
    }

    @Override
    public Map<String, Object> getParameters()
    {
        return params;
    }

    @Override
    public String toString()
    {
        return query;
    }
}
